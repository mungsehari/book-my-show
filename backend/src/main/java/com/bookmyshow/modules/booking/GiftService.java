package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gift a ticket: the booking moves to another user, with no payment. The ticket code is tied to the booking, so the
 * new owner sees the same QR in their ticket. Closes with cancels, 2 hours before the show.
 */
@Service
public class GiftService {

    private final RefundService refundService;
    private final UserService userService;
    private final Notifier notifier;

    public GiftService(RefundService refundService, UserService userService, Notifier notifier) {
        this.refundService = refundService;
        this.userService = userService;
        this.notifier = notifier;
    }

    @Transactional
    public BookingResponse gift(long bookingId, String email, CurrentUser user) {
        Booking booking = refundService.lockOwned(bookingId, user);
        if (!BookingStatus.CONFIRMED.equals(booking.getStatus()) || booking.getCheckedInAt() != null) {
            throw new ConflictException("Only a confirmed, unused ticket can be given");
        }
        if (booking.isListedForResale()) {
            throw new ConflictException("This ticket is on resale. Stop the resale first.");
        }
        refundService.ensureBeforeCutoff(booking, "Gifting");
        long recipientId =
                userService.findIdByEmail(email).orElseThrow(() -> new NotFoundException("No user has this email"));
        if (recipientId == user.id()) {
            throw new BadRequestException("This ticket is already yours");
        }
        booking.giftTo(recipientId);
        notifier.notify(
                recipientId,
                "You got a ticket",
                "User " + user.id() + " gave you a ticket for " + booking.getMovieName() + " on "
                        + booking.getShowDate() + " at " + booking.getStartTime() + " (booking " + bookingId + ").");
        return BookingResponse.from(booking);
    }
}
