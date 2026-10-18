package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.waitlist.WaitlistService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancel with a full refund, and admin refunds for NEEDS_REFUND bookings. If Razorpay fails, nothing changes and the
 * call answers 502, so the user can try again.
 */
@Service
public class RefundService {

    /** Cancels and resale listings close this long before the show starts. */
    static final Duration CANCEL_BEFORE = Duration.ofHours(2);

    private final BookingRepository bookingRepository;
    private final ShowSeatBookingService seatBookingService;
    private final WaitlistService waitlistService;
    private final PaymentGateway paymentGateway;
    private final Notifier notifier;
    private final Clock clock;

    public RefundService(
            BookingRepository bookingRepository,
            ShowSeatBookingService seatBookingService,
            WaitlistService waitlistService,
            PaymentGateway paymentGateway,
            Notifier notifier,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.seatBookingService = seatBookingService;
        this.waitlistService = waitlistService;
        this.paymentGateway = paymentGateway;
        this.notifier = notifier;
        this.clock = clock;
    }

    /** The owner cancels. Full refund, the seats are freed, and the waitlist gets first chance at them. */
    @Transactional
    public BookingResponse cancel(long bookingId, CurrentUser user) {
        Booking booking = lockOwned(bookingId, user);
        if (!BookingStatus.CONFIRMED.equals(booking.getStatus())) {
            throw new ConflictException(
                    "Booking " + bookingId + " is " + booking.getStatus() + ", so it cannot be cancelled");
        }
        if (booking.getCheckedInAt() != null) {
            throw new ConflictException("This ticket was already used at the entry");
        }
        ensureBeforeCutoff(booking, "Cancelling");

        String refundId = paymentGateway.refund(
                booking.getRazorpayPaymentId(), BookingService.toPaise(booking), "Cancelled booking " + bookingId);
        booking.cancel(refundId, clock.instant());
        List<Long> freed = seatBookingService.releaseSeats(booking.getShowId(), bookingId);
        waitlistService.offerSeats(booking.getShowId(), freed);
        notifier.notify(
                booking.getUserId(),
                "Booking cancelled",
                "Booking " + bookingId + " is cancelled. " + booking.getAmount() + " " + booking.getCurrency()
                        + " is on its way back to you.");
        return BookingResponse.from(booking);
    }

    /** Admin: pays back a booking that was paid after its seats were sold to someone else. */
    @Transactional
    public BookingResponse refundNeedsRefund(long bookingId) {
        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        if (!BookingStatus.NEEDS_REFUND.equals(booking.getStatus())) {
            throw new ConflictException("Booking " + bookingId + " is " + booking.getStatus() + ", not NEEDS_REFUND");
        }
        String refundId = paymentGateway.refund(
                booking.getRazorpayPaymentId(),
                BookingService.toPaise(booking),
                "Seats sold out, booking " + bookingId);
        booking.markRefunded(refundId, clock.instant());
        notifier.notify(
                booking.getUserId(),
                "Refund sent",
                "Booking " + bookingId + " could not get its seats. " + booking.getAmount() + " "
                        + booking.getCurrency() + " is on its way back to you.");
        return BookingResponse.from(booking);
    }

    Booking lockOwned(long bookingId, CurrentUser user) {
        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));
        if (booking.getUserId() != user.id()) {
            throw new ForbiddenException("This booking belongs to another user");
        }
        return booking;
    }

    void ensureBeforeCutoff(Booking booking, String action) {
        LocalDateTime start = LocalDateTime.of(booking.getShowDate(), booking.getStartTime());
        if (!LocalDateTime.now(clock).plus(CANCEL_BEFORE).isBefore(start)) {
            throw new BadRequestException(
                    action + " closes " + CANCEL_BEFORE.toHours() + " hours before the show starts");
        }
    }
}
