package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.modules.waitlist.WaitlistService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GiftServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private BookingRepository bookingRepository;
    private UserService userService;
    private Notifier notifier;
    private GiftService giftService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        userService = mock(UserService.class);
        notifier = mock(Notifier.class);
        RefundService refundService = new RefundService(
                bookingRepository,
                mock(ShowSeatBookingService.class),
                mock(WaitlistService.class),
                mock(PaymentGateway.class),
                notifier,
                Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC));
        giftService = new GiftService(refundService, userService, notifier);
    }

    @Test
    void ticketMovesToFriend() {
        Booking booking = RefundServiceTest.confirmed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(userService.findIdByEmail("ravi@example.com")).thenReturn(Optional.of(8L));

        giftService.gift(9L, "ravi@example.com", ASHA);

        assertThat(booking.getUserId()).isEqualTo(8L);
        assertThat(booking.getGiftedFromUserId()).isEqualTo(7L);
        verify(notifier).notify(eq(8L), anyString(), anyString());
    }

    @Test
    void unknownEmailIs404() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(RefundServiceTest.confirmed()));
        when(userService.findIdByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> giftService.gift(9L, "nobody@example.com", ASHA))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cannotGiftToYourself() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(RefundServiceTest.confirmed()));
        when(userService.findIdByEmail("asha@example.com")).thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> giftService.gift(9L, "asha@example.com", ASHA))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void ticketOnResaleCannotBeGiven() {
        Booking booking = RefundServiceTest.confirmed();
        booking.listForResale(Instant.parse("2026-10-18T08:00:00Z"));
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> giftService.gift(9L, "ravi@example.com", ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("resale");
    }
}
