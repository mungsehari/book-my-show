package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.booking.BookingTypes.MovieAudience;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import com.bookmyshow.modules.waitlist.WaitlistService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class BookingServiceTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final ConfirmPaymentRequest PAID = new ConfirmPaymentRequest("order_1", "pay_1", "sig");

    private BookingRepository bookingRepository;
    private ShowSeatLockService seatLockService;
    private ShowSeatBookingService seatBookingService;
    private PaymentGateway paymentGateway;
    private ResaleCompletion resaleCompletion;
    private WaitlistService waitlistService;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        seatLockService = mock(ShowSeatLockService.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        paymentGateway = mock(PaymentGateway.class);
        when(paymentGateway.publicKey()).thenReturn("rzp_test_abc");
        resaleCompletion = mock(ResaleCompletion.class);
        waitlistService = mock(WaitlistService.class);
        bookingService = new BookingService(
                bookingRepository,
                seatLockService,
                seatBookingService,
                paymentGateway,
                resaleCompletion,
                waitlistService,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void confirmTellsWaitlistTheOfferWasBooked() {
        Booking booking = pending();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.isValidPaymentSignature("order_1", "pay_1", "sig")).thenReturn(true);
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(true);

        bookingService.confirm(9L, PAID, ASHA);

        verify(waitlistService).markBooked(LOCK_ID);
    }

    @Test
    void resalePaymentGoesToResaleCompletion() {
        Booking seller = pending();
        seller.confirm("pay_0", Instant.now());
        Booking buyer = Booking.resaleOf(seller, 8L, "hold-1", Instant.now());

        bookingService.applyPayment(buyer, "pay_9");

        verify(resaleCompletion).complete(buyer, "pay_9");
        verify(seatBookingService, never()).sellSeats(anyLong(), any(), anyLong());
    }

    @Test
    void lateUnpaidCheckoutsExpire() {
        Booking stale = pending();
        when(bookingRepository.findTop100ByStatusAndCreatedAtBefore(
                        BookingStatus.PENDING_PAYMENT, Instant.parse("2026-10-18T04:45:00Z")))
                .thenReturn(List.of(stale));

        assertThat(bookingService.expireStale()).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void latePaymentOnExpiredBookingStillConfirms() {
        Booking expired = pending();
        expired.markExpired();
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(true);

        bookingService.applyPayment(expired, "pay_1");

        assertThat(expired.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void checkoutCreatesOrderInPaise() {
        when(seatLockService.getActiveLock(LOCK_ID, ASHA)).thenReturn(lock(List.of(1L, 3L), "650.00"));
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
        when(seatLockService.getActiveLock(LOCK_ID, ASHA)).thenReturn(lock(List.of(1L), "200.00"));
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

    @Test
    void upcomingHistoryStartsTodaySoonestFirst() {
        when(bookingRepository.findByUserIdAndShowDateGreaterThanEqualAndStatusIn(
                        eq(7L), eq(LocalDate.of(2026, 10, 18)), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pending()), PageRequest.of(0, 20), 1));

        PageResponse<BookingResponse> page = bookingService.listMine(ASHA, "upcoming", 0, 20);

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items())
                .singleElement()
                .satisfies(b -> assertThat(b.seatLabels()).containsExactly("A1"));
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Collection<String>> statuses = ArgumentCaptor.captor();
        verify(bookingRepository)
                .findByUserIdAndShowDateGreaterThanEqualAndStatusIn(
                        eq(7L), eq(LocalDate.of(2026, 10, 18)), statuses.capture(), pageable.capture());
        assertThat(statuses.getValue()).doesNotContain(BookingStatus.PENDING_PAYMENT);
        assertThat(pageable.getValue().getSort().getOrderFor("showDate").isAscending())
                .isTrue();
    }

    @Test
    void pastHistoryIsNewestFirst() {
        when(bookingRepository.findByUserIdAndShowDateLessThanAndStatusIn(
                        eq(7L), eq(LocalDate.of(2026, 10, 18)), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        assertThat(bookingService.listMine(ASHA, "past", 0, 20).items()).isEmpty();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findByUserIdAndShowDateLessThanAndStatusIn(eq(7L), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("showDate").isDescending())
                .isTrue();
    }

    @Test
    void audienceSplitsWatchedAndGoing() {
        // The clock is 2026-10-18 05:00. Show 18:00 today is upcoming. A checked-in booking counts as watched.
        Booking going = confirmedBooking();
        Booking watched = confirmedBooking();
        ReflectionTestUtils.setField(watched, "userId", 8L);
        watched.checkIn(2L, Instant.parse("2026-10-18T04:50:00Z"));
        when(bookingRepository.findByMovieIdAndStatusAndUserIdIn(2L, BookingStatus.CONFIRMED, Set.of(7L, 8L)))
                .thenReturn(List.of(going, watched));

        MovieAudience audience = bookingService.audienceOf(2L, Set.of(7L, 8L));

        assertThat(audience.going()).containsExactly(7L);
        assertThat(audience.watched()).containsExactly(8L);
    }

    @Test
    void watchedMeansShowStartedOrCheckedIn() {
        // The clock is 2026-10-18 05:00. The booking's show is 2026-10-18 18:00.
        Booking later = confirmedBooking();
        when(bookingRepository.findByUserIdAndMovieIdAndStatusAndShowDateLessThanEqual(
                        7L, 2L, BookingStatus.CONFIRMED, LocalDate.of(2026, 10, 18)))
                .thenReturn(List.of(later));

        assertThat(bookingService.findWatchedBooking(7L, 2L)).isEmpty();

        later.checkIn(2L, Instant.parse("2026-10-18T04:50:00Z"));
        assertThat(bookingService.findWatchedBooking(7L, 2L)).contains(9L);
    }

    private static Booking confirmedBooking() {
        Booking booking = pending();
        booking.confirm("pay_1", Instant.parse("2026-10-17T05:00:00Z"));
        return booking;
    }

    static ActiveLock lock(List<Long> seatIds, String total) {
        return new ActiveLock(
                LOCK_ID,
                7L,
                50L,
                seatIds,
                new BigDecimal(total),
                10L,
                "Screen 1",
                "Sky Riders",
                LocalDate.of(2026, 10, 18),
                LocalTime.of(18, 0),
                seatIds.stream().map(id -> "A" + id).toList(),
                2L);
    }

    static Booking pending() {
        Booking booking = new Booking(lock(List.of(1L), "200.00"), "INR", Instant.now());
        booking.attachOrder("order_1");
        return withId(booking, 9L);
    }

    private static Booking withId(Booking booking, long id) {
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }
}
