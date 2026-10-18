package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BookingServiceTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final ConfirmPaymentRequest PAID = new ConfirmPaymentRequest("order_1", "pay_1", "sig");

    private BookingRepository bookingRepository;
    private ShowSeatLockService seatLockService;
    private ShowSeatBookingService seatBookingService;
    private PaymentGateway paymentGateway;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        seatLockService = mock(ShowSeatLockService.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        paymentGateway = mock(PaymentGateway.class);
        when(paymentGateway.publicKey()).thenReturn("rzp_test_abc");
        bookingService = new BookingService(
                bookingRepository,
                seatLockService,
                seatBookingService,
                paymentGateway,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void checkoutCreatesOrderInPaise() {
        when(seatLockService.getActiveLock(LOCK_ID, ASHA))
                .thenReturn(new ActiveLock(LOCK_ID, 7L, 50L, List.of(1L, 3L), new BigDecimal("650.00")));
        when(bookingRepository.findByLockId(LOCK_ID)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> withId(call.getArgument(0), 9L));
        when(paymentGateway.createOrder(65000, "INR", "booking-9"))
                .thenReturn(new PaymentOrder("order_1", 65000, "INR"));

        CheckoutResponse response = bookingService.checkout(LOCK_ID, ASHA);

        assertThat(response.bookingId()).isEqualTo(9L);
        assertThat(response.amountInPaise()).isEqualTo(65000);
        assertThat(response.razorpayOrderId()).isEqualTo("order_1");
        assertThat(response.razorpayKeyId()).isEqualTo("rzp_test_abc");
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void checkoutAgainReusesOrder() {
        when(seatLockService.getActiveLock(LOCK_ID, ASHA))
                .thenReturn(new ActiveLock(LOCK_ID, 7L, 50L, List.of(1L), new BigDecimal("200.00")));
        when(bookingRepository.findByLockId(LOCK_ID)).thenReturn(Optional.of(pending()));

        assertThat(bookingService.checkout(LOCK_ID, ASHA).razorpayOrderId()).isEqualTo("order_1");
        verify(paymentGateway, never()).createOrder(anyLong(), anyString(), anyString());
    }

    @Test
    void confirmSellsSeatsAndFreesLock() {
        Booking booking = pending();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.isValidPaymentSignature("order_1", "pay_1", "sig")).thenReturn(true);
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(true);

        BookingResponse response = bookingService.confirm(9L, PAID, ASHA);

        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        verify(seatLockService).releaseAfterSale(LOCK_ID);
    }

    @Test
    void confirmRejectsBadSignature() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(pending()));
        when(paymentGateway.isValidPaymentSignature("order_1", "pay_1", "sig")).thenReturn(false);

        assertThatThrownBy(() -> bookingService.confirm(9L, PAID, ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Payment signature is invalid");
        verify(seatBookingService, never()).sellSeats(anyLong(), any(), anyLong());
    }

    @Test
    void confirmRejectsOtherOrder() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> bookingService.confirm(9L, new ConfirmPaymentRequest("order_2", "pay_1", "sig"), ASHA))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void confirmRejectsOtherUser() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(pending()));

        assertThatThrownBy(() -> bookingService.confirm(9L, PAID, new CurrentUser(8L, Roles.USER)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void paidButSoldOutNeedsRefund() {
        Booking booking = pending();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.isValidPaymentSignature("order_1", "pay_1", "sig")).thenReturn(true);
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.confirm(9L, PAID, ASHA))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("will be refunded");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.NEEDS_REFUND);
    }

    @Test
    void secondPaymentForConfirmedBookingChangesNothing() {
        Booking booking = pending();
        booking.confirm("pay_1", Instant.now());

        bookingService.applyPayment(booking, "pay_1");

        verify(seatBookingService, never()).sellSeats(anyLong(), any(), anyLong());
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void adminCanViewAnyBooking() {
        when(bookingRepository.findById(9L)).thenReturn(Optional.of(pending()));

        assertThat(bookingService.get(9L, new CurrentUser(1L, Roles.ADMIN)).id())
                .isEqualTo(9L);
        assertThatThrownBy(() -> bookingService.get(9L, new CurrentUser(8L, Roles.USER)))
                .isInstanceOf(ForbiddenException.class);
    }

    static Booking pending() {
        Booking booking = new Booking(7L, 50L, List.of(1L), new BigDecimal("200.00"), "INR", LOCK_ID, Instant.now());
        booking.attachOrder("order_1");
        return withId(booking, 9L);
    }

    private static Booking withId(Booking booking, long id) {
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }
}
