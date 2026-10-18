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
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.booking.BookingTypes.SplitResponse;
import com.bookmyshow.modules.friend.FriendService;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SplitServiceTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final CurrentUser RAVI = new CurrentUser(8L, Roles.USER);

    private BookingRepository bookingRepository;
    private PaymentShareRepository shareRepository;
    private BookingMemberRepository memberRepository;
    private ShowSeatLockService seatLockService;
    private ShowSeatBookingService seatBookingService;
    private FriendService friendService;
    private PaymentGateway paymentGateway;
    private SplitService splitService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        shareRepository = mock(PaymentShareRepository.class);
        memberRepository = mock(BookingMemberRepository.class);
        seatLockService = mock(ShowSeatLockService.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        friendService = mock(FriendService.class);
        paymentGateway = mock(PaymentGateway.class);
        splitService = new SplitService(
                bookingRepository,
                shareRepository,
                memberRepository,
                seatLockService,
                seatBookingService,
                friendService,
                paymentGateway,
                mock(Notifier.class),
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void organizerPaysTheLeftoverPaise() {
        assertThat(SplitService.splitPaise(10000, 3)).containsExactly(3334, 3333, 3333);
        assertThat(SplitService.splitPaise(20000, 2)).containsExactly(10000, 10000);
    }

    @Test
    void createsOneOrderPerPersonAndExtendsLock() {
        when(friendService.friendIds(7L)).thenReturn(Set.of(8L, 9L));
        when(bookingRepository.findByLockId(LOCK_ID)).thenReturn(Optional.empty());
        when(seatLockService.getActiveLock(LOCK_ID, ASHA)).thenReturn(BookingServiceTest.lock(List.of(1L), "100.00"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> withId(call.getArgument(0)));
        AtomicLong ids = new AtomicLong(100);
        when(shareRepository.save(any(PaymentShare.class))).thenAnswer(call -> {
            PaymentShare share = call.getArgument(0);
            ReflectionTestUtils.setField(share, "id", ids.incrementAndGet());
            return share;
        });
        when(paymentGateway.createOrder(anyLong(), eq("INR"), anyString()))
                .thenAnswer(call -> new PaymentOrder("order_" + call.getArgument(2), call.getArgument(0), "INR"));

        SplitResponse split = splitService.create(LOCK_ID, List.of(8L, 9L), ASHA);

        verify(seatLockService).extendLock(LOCK_ID, ASHA, SplitService.SPLIT_WINDOW);
        assertThat(split.shares()).extracting("userId").containsExactly(7L, 8L, 9L);
        assertThat(split.shares()).extracting("amountInPaise").containsExactly(3334L, 3333L, 3333L);
        assertThat(split.shares().get(1).razorpayOrderId()).isEqualTo("order_share-102");
        assertThat(split.expiresAt()).isEqualTo(Instant.parse("2026-10-18T05:15:00Z"));
    }

    @Test
    void onlyFriendsCanJoinSplit() {
        when(friendService.friendIds(7L)).thenReturn(Set.of(8L));

        assertThatThrownBy(() -> splitService.create(LOCK_ID, List.of(9L), ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User 9 is not your friend");
        verify(seatLockService, never()).extendLock(anyString(), any(), any());
    }

    @Test
    void lastPaymentSellsSeats() {
        Booking booking = splitBooking();
        PaymentShare mine = share(101L, 7L, "50.00");
        mine.markPaid("pay_a", Instant.now());
        PaymentShare ravis = share(102L, 8L, "50.00");
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(shareRepository.findByBookingIdOrderByIdAsc(9L)).thenReturn(List.of(mine, ravis));
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(true);

        splitService.applyPayment(ravis, "pay_b");

        assertThat(ravis.isPaid()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(seatLockService).releaseAfterSale(booking.getLockId());
    }

    @Test
    void earlyPaymentWaitsForOthers() {
        Booking booking = splitBooking();
        PaymentShare mine = share(101L, 7L, "50.00");
        PaymentShare ravis = share(102L, 8L, "50.00");
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(shareRepository.findByBookingIdOrderByIdAsc(9L)).thenReturn(List.of(mine, ravis));

        splitService.applyPayment(mine, "pay_a");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verify(seatBookingService, never()).sellSeats(anyLong(), any(), anyLong());
    }

    @Test
    void lostSeatsRefundEveryone() {
        Booking booking = splitBooking();
        PaymentShare mine = share(101L, 7L, "50.00");
        mine.markPaid("pay_a", Instant.now());
        PaymentShare ravis = share(102L, 8L, "50.00");
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(shareRepository.findByBookingIdOrderByIdAsc(9L)).thenReturn(List.of(mine, ravis));
        when(seatBookingService.sellSeats(50L, List.of(1L), 9L)).thenReturn(false);
        when(paymentGateway.refund(anyString(), eq(5000L), anyString())).thenReturn("rfnd");

        splitService.applyPayment(ravis, "pay_b");

        assertThat(mine.getStatus()).isEqualTo(PaymentShare.REFUNDED);
        assertThat(ravis.getStatus()).isEqualTo(PaymentShare.REFUNDED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REFUNDED);
    }

    @Test
    void latePaymentOnExpiredSplitIsRefunded() {
        Booking booking = splitBooking();
        booking.markExpired();
        PaymentShare ravis = share(102L, 8L, "50.00");
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));
        when(paymentGateway.refund("pay_b", 5000L, "Split booking 9 did not complete"))
                .thenReturn("rfnd");

        splitService.applyPayment(ravis, "pay_b");

        assertThat(ravis.getStatus()).isEqualTo(PaymentShare.REFUNDED);
    }

    @Test
    void secondPaymentOnSameShareChangesNothing() {
        PaymentShare paid = share(101L, 7L, "50.00");
        paid.markPaid("pay_a", Instant.now());

        splitService.applyPayment(paid, "pay_a");

        verify(bookingRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void expiryRefundsPaidAndVoidsUnpaid() {
        Booking booking = splitBooking();
        PaymentShare paid = share(101L, 7L, "50.00");
        paid.markPaid("pay_a", Instant.now());
        PaymentShare unpaid = share(102L, 8L, "50.00");
        when(bookingRepository.findTop100ByStatusAndSplitTrueAndCreatedAtBefore(
                        BookingStatus.PENDING_PAYMENT, Instant.parse("2026-10-18T04:45:00Z")))
                .thenReturn(List.of(booking));
        when(shareRepository.findByBookingIdOrderByIdAsc(9L)).thenReturn(List.of(paid, unpaid));
        when(paymentGateway.refund(anyString(), anyLong(), anyString())).thenReturn("rfnd");

        assertThat(splitService.expireStale()).isEqualTo(1);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(paid.getStatus()).isEqualTo(PaymentShare.REFUNDED);
        assertThat(unpaid.getStatus()).isEqualTo(PaymentShare.VOID);
        verify(seatLockService).releaseAfterSale(booking.getLockId());
    }

    @Test
    void onlyOwnerConfirmsShare() {
        when(shareRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(share(101L, 7L, "50.00")));

        assertThatThrownBy(() -> splitService.confirm(101L, new ConfirmPaymentRequest("order_x", "pay", "sig"), RAVI))
                .isInstanceOf(ForbiddenException.class);
    }

    private static Booking splitBooking() {
        Booking booking = BookingServiceTest.pending();
        booking.markSplit();
        return booking;
    }

    private static PaymentShare share(long id, long userId, String amount) {
        PaymentShare share = new PaymentShare(9L, userId, new BigDecimal(amount), "INR", Instant.now());
        ReflectionTestUtils.setField(share, "id", id);
        share.attachOrder("order_" + id);
        return share;
    }

    private static Booking withId(Booking booking) {
        ReflectionTestUtils.setField(booking, "id", 9L);
        return booking;
    }
}
