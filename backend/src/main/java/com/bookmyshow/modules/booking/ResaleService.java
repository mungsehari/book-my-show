package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ResaleListing;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Face-value resale. An owner offers a whole ticket. A buyer pays exactly what the owner paid, never more, so there is
 * no black market. Once the buyer's payment is confirmed, the seats move to the buyer and the owner is refunded in full
 * (see {@link ResaleCompletion}).
 */
@Service
@Transactional(readOnly = true)
public class ResaleService {

    /** One buyer at a time: a buyer's unpaid checkout holds the ticket this long. */
    static final Duration BUYER_HOLD = Duration.ofMinutes(10);

    private final BookingRepository bookingRepository;
    private final RefundService refundService;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public ResaleService(
            BookingRepository bookingRepository,
            RefundService refundService,
            PaymentGateway paymentGateway,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.refundService = refundService;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse list(long bookingId, CurrentUser user) {
        Booking booking = refundService.lockOwned(bookingId, user);
        if (!BookingStatus.CONFIRMED.equals(booking.getStatus()) || booking.getCheckedInAt() != null) {
            throw new ConflictException("Only a confirmed, unused ticket can be resold");
        }
        RefundService.ensureNotSplit(booking);
        refundService.ensureBeforeCutoff(booking, "Resale");
        if (!booking.isListedForResale()) {
            booking.listForResale(clock.instant());
        }
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse unlist(long bookingId, CurrentUser user) {
        Booking booking = refundService.lockOwned(bookingId, user);
        if (activeBuyer(booking.getId())) {
            throw new ConflictException("Someone is paying for this ticket right now. Try again in a few minutes.");
        }
        booking.unlistResale();
        return BookingResponse.from(booking);
    }

    /** Public list of resale tickets for a show, oldest offer first. */
    public List<ResaleListing> listings(long showId) {
        return bookingRepository
                .findByShowIdAndStatusAndResaleListedAtIsNotNullOrderByResaleListedAtAsc(
                        showId, BookingStatus.CONFIRMED)
                .stream()
                .filter(booking -> booking.getCheckedInAt() == null)
                .map(booking -> new ResaleListing(
                        booking.getId(),
                        booking.getShowId(),
                        booking.getSeatLabels(),
                        booking.getAmount(),
                        booking.getCurrency(),
                        activeBuyer(booking.getId())))
                .toList();
    }

    /** Starts payment for a resale ticket at its face value. */
    @Transactional
    public CheckoutResponse checkout(long sellerBookingId, CurrentUser buyer) {
        Booking seller = bookingRepository
                .findByIdForUpdate(sellerBookingId)
                .orElseThrow(() -> new NotFoundException("Resale ticket " + sellerBookingId + " not found"));
        if (!BookingStatus.CONFIRMED.equals(seller.getStatus()) || !seller.isListedForResale()) {
            throw new NotFoundException("Resale ticket " + sellerBookingId + " not found");
        }
        if (seller.getUserId() == buyer.id()) {
            throw new BadRequestException("You cannot buy your own ticket");
        }
        Booking pending = bookingRepository
                .findFirstByResaleOfBookingIdAndStatusAndCreatedAtAfter(
                        sellerBookingId,
                        BookingStatus.PENDING_PAYMENT,
                        clock.instant().minus(BUYER_HOLD))
                .orElse(null);
        if (pending != null && pending.getUserId() != buyer.id()) {
            throw new ConflictException("Someone is paying for this ticket right now. Try again in a few minutes.");
        }
        Booking booking = pending;
        if (booking == null) {
            booking = bookingRepository.save(
                    Booking.resaleOf(seller, buyer.id(), UUID.randomUUID().toString(), clock.instant()));
            PaymentOrder order = paymentGateway.createOrder(
                    BookingService.toPaise(booking), booking.getCurrency(), "booking-" + booking.getId());
            booking.attachOrder(order.id());
        }
        return new CheckoutResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getAmount(),
                BookingService.toPaise(booking),
                booking.getCurrency(),
                booking.getRazorpayOrderId(),
                paymentGateway.publicKey());
    }

    private boolean activeBuyer(long sellerBookingId) {
        return bookingRepository
                .findFirstByResaleOfBookingIdAndStatusAndCreatedAtAfter(
                        sellerBookingId,
                        BookingStatus.PENDING_PAYMENT,
                        clock.instant().minus(BUYER_HOLD))
                .isPresent();
    }
}
