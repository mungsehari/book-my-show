package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout: turns a seat lock into a paid booking.
 *
 * <ol>
 *   <li>{@link #checkout}: makes a Razorpay order for the locked seats.
 *   <li>The user pays in Razorpay checkout on the frontend.
 *   <li>{@link #confirm} (from the frontend) or the webhook: checks the payment and sells the seats.
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class BookingService {

    static final String CURRENCY = "INR";
    static final String UPCOMING = "upcoming";

    /** Shown in history. PENDING_PAYMENT is left out, because unpaid checkouts are noise. */
    private static final List<String> HISTORY_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.FAILED, BookingStatus.NEEDS_REFUND);

    private final BookingRepository bookingRepository;
    private final ShowSeatLockService seatLockService;
    private final ShowSeatBookingService seatBookingService;
    private final PaymentGateway paymentGateway;
    private final Clock clock;

    public BookingService(
            BookingRepository bookingRepository,
            ShowSeatLockService seatLockService,
            ShowSeatBookingService seatBookingService,
            PaymentGateway paymentGateway,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.seatLockService = seatLockService;
        this.seatBookingService = seatBookingService;
        this.paymentGateway = paymentGateway;
        this.clock = clock;
    }

    /** Starts payment for a held lock. Calling it again for the same lock returns the same order. */
    @Transactional
    public CheckoutResponse checkout(String lockId, CurrentUser user) {
        ActiveLock lock = seatLockService.getActiveLock(lockId, user);
        Booking booking = bookingRepository.findByLockId(lockId).orElse(null);
        if (booking == null) {
            booking = bookingRepository.save(new Booking(lock, CURRENCY, clock.instant()));
            PaymentOrder order = paymentGateway.createOrder(toPaise(booking), CURRENCY, "booking-" + booking.getId());
            booking.attachOrder(order.id());
        } else if (!booking.awaitsPayment()) {
            throw new ConflictException("Booking " + booking.getId() + " is already " + booking.getStatus());
        }
        return new CheckoutResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getAmount(),
                toPaise(booking),
                booking.getCurrency(),
                booking.getRazorpayOrderId(),
                paymentGateway.publicKey());
    }

    /**
     * Checks what Razorpay checkout returned and sells the seats. A NEEDS_REFUND result is saved even though the call
     * answers 409, so the refund is never lost.
     */
    @Transactional(noRollbackFor = ConflictException.class)
    public BookingResponse confirm(long bookingId, ConfirmPaymentRequest request, CurrentUser user) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElseThrow(() -> notFound(bookingId));
        if (booking.getUserId() != user.id()) {
            throw new ForbiddenException("This booking belongs to another user");
        }
        if (!request.razorpayOrderId().equals(booking.getRazorpayOrderId())) {
            throw new BadRequestException("Order does not match this booking");
        }
        if (!paymentGateway.isValidPaymentSignature(
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new BadRequestException("Payment signature is invalid");
        }
        applyPayment(booking, request.razorpayPaymentId());
        if (BookingStatus.NEEDS_REFUND.equals(booking.getStatus())) {
            throw new ConflictException("Your seats were sold to someone else before the payment arrived. " + "Booking "
                    + bookingId + " will be refunded.");
        }
        return BookingResponse.from(booking);
    }

    /**
     * The user's bookings. "upcoming" is shows from today on, soonest first. "past" is older shows, newest first.
     * Unpaid checkouts are left out.
     */
    public PageResponse<BookingResponse> listMine(CurrentUser user, String when, int page, int size) {
        LocalDate today = LocalDate.now(clock);
        Page<Booking> result;
        if (UPCOMING.equals(when)) {
            Sort soonest = Sort.by("showDate", "startTime").ascending();
            result = bookingRepository.findByUserIdAndShowDateGreaterThanEqualAndStatusIn(
                    user.id(), today, HISTORY_STATUSES, PageRequest.of(page, size, soonest));
        } else {
            Sort newest = Sort.by("showDate", "startTime").descending();
            result = bookingRepository.findByUserIdAndShowDateLessThanAndStatusIn(
                    user.id(), today, HISTORY_STATUSES, PageRequest.of(page, size, newest));
        }
        return PageResponse.of(result, BookingResponse::from);
    }

    public BookingResponse get(long bookingId, CurrentUser user) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> notFound(bookingId));
        if (booking.getUserId() != user.id() && !user.isAdmin()) {
            throw new ForbiddenException("This booking belongs to another user");
        }
        return BookingResponse.from(booking);
    }

    /**
     * Applies a verified payment. Safe to call twice (confirm call and webhook): a confirmed booking is left as is.
     * The caller must hold the booking row lock.
     */
    void applyPayment(Booking booking, String paymentId) {
        if (!booking.awaitsPayment()) {
            return;
        }
        boolean sold = seatBookingService.sellSeats(booking.getShowId(), booking.getSeatIds(), booking.getId());
        if (sold) {
            booking.confirm(paymentId, clock.instant());
            seatLockService.releaseAfterSale(booking.getLockId());
        } else {
            booking.markNeedsRefund(paymentId);
        }
    }

    private static long toPaise(Booking booking) {
        return booking.getAmount().movePointRight(2).longValueExact();
    }

    private static NotFoundException notFound(long bookingId) {
        return new NotFoundException("Booking " + bookingId + " not found");
    }
}
