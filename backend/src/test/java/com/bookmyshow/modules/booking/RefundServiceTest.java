package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.UpstreamException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.waitlist.WaitlistService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefundServiceTest {

    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private BookingRepository bookingRepository;
    private ShowSeatBookingService seatBookingService;
    private WaitlistService waitlistService;
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        waitlistService = mock(WaitlistService.class);
        paymentGateway = mock(PaymentGateway.class);
    }

    @Test
    void cancelRefundsFreesSeatsAndOffersThemToWaitlist() {
        Booking booking = confirmed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.refund("pay_1", 20000, "Cancelled booking 9")).thenReturn("rfnd_1");
        when(seatBookingService.releaseSeats(50L, 9L)).thenReturn(List.of(1L));

        // The show is 2026-10-18 18:00. The clock is 10:00 the same day: 8 hours before.
        assertThat(service("2026-10-18T10:00:00Z").cancel(9L, ASHA).status()).isEqualTo(BookingStatus.CANCELLED);

        verify(waitlistService).offerSeats(50L, List.of(1L));
    }

    @Test
    void cancelClosesTwoHoursBeforeShow() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(confirmed()));

        assertThatThrownBy(() -> service("2026-10-18T16:30:00Z").cancel(9L, ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cancelling closes 2 hours before the show starts");
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void splitBookingCannotBeCancelledYet() {
        Booking booking = confirmed();
        booking.markSplit();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").cancel(9L, ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("split booking");
        verifyNoInteractions(paymentGateway);
    }

    @Test
    void usedTicketCannotBeCancelled() {
        Booking booking = confirmed();
        booking.checkIn(2L, Instant.now());
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").cancel(9L, ASHA))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unpaidBookingCannotBeCancelled() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BookingServiceTest.pending()));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").cancel(9L, ASHA))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void otherUserCannotCancel() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(confirmed()));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").cancel(9L, new CurrentUser(8L, Roles.USER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void failedRefundChangesNothing() {
        Booking booking = confirmed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.refund(anyString(), anyLong(), anyString()))
                .thenThrow(new UpstreamException("Could not refund the payment. Try again.", null));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").cancel(9L, ASHA))
                .isInstanceOf(UpstreamException.class);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(seatBookingService, never()).releaseSeats(anyLong(), anyLong());
    }

    @Test
    void adminRefundsNeedsRefundBooking() {
        Booking booking = BookingServiceTest.pending();
        booking.markNeedsRefund("pay_1");
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.refund("pay_1", 20000, "Seats sold out, booking 9")).thenReturn("rfnd_2");

        assertThat(service("2026-10-18T10:00:00Z").refundNeedsRefund(9L).status())
                .isEqualTo(BookingStatus.REFUNDED);
    }

    @Test
    void adminRefundOnlyForNeedsRefund() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(confirmed()));

        assertThatThrownBy(() -> service("2026-10-18T10:00:00Z").refundNeedsRefund(9L))
                .isInstanceOf(ConflictException.class);
    }

    private RefundService service(String now) {
        return new RefundService(
                bookingRepository,
                seatBookingService,
                waitlistService,
                paymentGateway,
                mock(Notifier.class),
                Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }

    static Booking confirmed() {
        Booking booking = BookingServiceTest.pending();
        booking.confirm("pay_1", Instant.parse("2026-10-17T05:00:00Z"));
        return booking;
    }
}
