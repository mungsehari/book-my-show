package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

class ResaleServiceTest {

    private static final CurrentUser SELLER = new CurrentUser(7L, Roles.USER);
    private static final CurrentUser BUYER = new CurrentUser(8L, Roles.USER);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);

    private BookingRepository bookingRepository;
    private PaymentGateway paymentGateway;
    private ResaleService resaleService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        paymentGateway = mock(PaymentGateway.class);
        RefundService refundService = new RefundService(
                bookingRepository,
                mock(ShowSeatBookingService.class),
                mock(WaitlistService.class),
                paymentGateway,
                mock(Notifier.class),
                CLOCK);
        resaleService = new ResaleService(bookingRepository, refundService, paymentGateway, CLOCK);
    }

    @Test
    void ownerListsTicket() {
        Booking booking = RefundServiceTest.confirmed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(booking));

        resaleService.list(9L, SELLER);

        assertThat(booking.isListedForResale()).isTrue();
    }

    @Test
    void cannotListUnpaidTicket() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(BookingServiceTest.pending()));

        assertThatThrownBy(() -> resaleService.list(9L, SELLER)).isInstanceOf(ConflictException.class);
    }

    @Test
    void buyerPaysFaceValue() {
        Booking seller = listed();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(seller));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> {
            Booking saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 12L);
            return saved;
        });
        when(paymentGateway.createOrder(eq(20000L), eq("INR"), anyString()))
                .thenReturn(new PaymentOrder("order_9", 20000, "INR"));

        CheckoutResponse checkout = resaleService.checkout(9L, BUYER);

        assertThat(checkout.bookingId()).isEqualTo(12L);
        assertThat(checkout.amount()).isEqualByComparingTo("200.00");
        assertThat(checkout.razorpayOrderId()).isEqualTo("order_9");
    }

    @Test
    void cannotBuyOwnTicket() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(listed()));

        assertThatThrownBy(() -> resaleService.checkout(9L, SELLER)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void oneBuyerAtATime() {
        Booking seller = listed();
        Booking otherBuyer = Booking.resaleOf(seller, 99L, "hold-1", Instant.parse("2026-10-18T09:55:00Z"));
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(seller));
        when(bookingRepository.findFirstByResaleOfBookingIdAndStatusAndCreatedAtAfter(
                        eq(9L), eq(BookingStatus.PENDING_PAYMENT), any()))
                .thenReturn(Optional.of(otherBuyer));

        assertThatThrownBy(() -> resaleService.checkout(9L, BUYER)).isInstanceOf(ConflictException.class);
    }

    @Test
    void unlistedTicketIsNotForSale() {
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(RefundServiceTest.confirmed()));

        assertThatThrownBy(() -> resaleService.checkout(9L, BUYER)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listingsShowFaceValue() {
        when(bookingRepository.findByShowIdAndStatusAndResaleListedAtIsNotNullOrderByResaleListedAtAsc(
                        50L, BookingStatus.CONFIRMED))
                .thenReturn(List.of(listed()));

        assertThat(resaleService.listings(50L)).singleElement().satisfies(listing -> {
            assertThat(listing.price()).isEqualByComparingTo("200.00");
            assertThat(listing.seats()).containsExactly("A1");
            assertThat(listing.onHold()).isFalse();
        });
    }

    private static Booking listed() {
        Booking booking = RefundServiceTest.confirmed();
        booking.listForResale(Instant.parse("2026-10-18T08:00:00Z"));
        return booking;
    }
}
