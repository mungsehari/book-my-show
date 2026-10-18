package com.bookmyshow.modules.booking;

import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Finishes a resale once the buyer has paid: moves the seats to the buyer, refunds the seller in full, and marks the
 * seller's booking RESOLD. If the seller's ticket is no longer on offer (cancelled, used, or sold), the buyer is set to
 * NEEDS_REFUND instead. Runs inside the caller's transaction, which holds the buyer's booking row lock.
 */
@Component
class ResaleCompletion {

    private final BookingRepository bookingRepository;
    private final ShowSeatBookingService seatBookingService;
    private final PaymentGateway paymentGateway;
    private final Notifier notifier;
    private final Clock clock;

    ResaleCompletion(
            BookingRepository bookingRepository,
            ShowSeatBookingService seatBookingService,
            PaymentGateway paymentGateway,
            Notifier notifier,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.seatBookingService = seatBookingService;
        this.paymentGateway = paymentGateway;
        this.notifier = notifier;
        this.clock = clock;
    }

    void complete(Booking buyer, String paymentId) {
        Booking seller = bookingRepository
                .findByIdForUpdate(buyer.getResaleOfBookingId())
                .orElse(null);
        boolean onOffer = seller != null
                && BookingStatus.CONFIRMED.equals(seller.getStatus())
                && seller.isListedForResale()
                && seller.getCheckedInAt() == null;
        if (!onOffer || !seatBookingService.transferSeats(seller.getShowId(), seller.getId(), buyer.getId())) {
            buyer.markNeedsRefund(paymentId);
            return;
        }
        String refundId = paymentGateway.refund(
                seller.getRazorpayPaymentId(),
                BookingService.toPaise(seller),
                "Ticket resold at face value, booking " + seller.getId());
        seller.markResold(refundId, clock.instant());
        buyer.confirm(paymentId, clock.instant());
        notifier.notify(
                seller.getUserId(),
                "Your ticket was resold",
                "Booking " + seller.getId() + " was bought by another user. " + seller.getAmount() + " "
                        + seller.getCurrency() + " is on its way back to you.");
    }
}
