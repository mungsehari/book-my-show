package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ResaleCompletionTest {

    private BookingRepository bookingRepository;
    private ShowSeatBookingService seatBookingService;
    private PaymentGateway paymentGateway;
    private ResaleCompletion completion;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        seatBookingService = mock(ShowSeatBookingService.class);
        paymentGateway = mock(PaymentGateway.class);
        completion = new ResaleCompletion(
                bookingRepository,
                seatBookingService,
                paymentGateway,
                mock(Notifier.class),
                Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void movesSeatsAndRefundsSeller() {
        Booking seller = RefundServiceTest.confirmed();
        seller.listForResale(Instant.parse("2026-10-18T08:00:00Z"));
        Booking buyer = buyerOf(seller);
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(seller));
        when(seatBookingService.transferSeats(50L, 9L, 12L)).thenReturn(true);
        when(paymentGateway.refund("pay_1", 20000, "Ticket resold at face value, booking 9"))
                .thenReturn("rfnd_1");

        completion.complete(buyer, "pay_9");

        assertThat(buyer.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(seller.getStatus()).isEqualTo(BookingStatus.RESOLD);
        assertThat(seller.isListedForResale()).isFalse();
    }

    @Test
    void buyerNeedsRefundIfTicketWasWithdrawn() {
        Booking seller = RefundServiceTest.confirmed();
        seller.listForResale(Instant.parse("2026-10-18T08:00:00Z"));
        Booking buyer = buyerOf(seller);
        seller.unlistResale();
        when(bookingRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(seller));

        completion.complete(buyer, "pay_9");

        assertThat(buyer.getStatus()).isEqualTo(BookingStatus.NEEDS_REFUND);
        assertThat(seller.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentGateway, never()).refund(anyString(), anyLong(), anyString());
    }

    private static Booking buyerOf(Booking seller) {
        Booking buyer = Booking.resaleOf(seller, 8L, "hold-1", Instant.parse("2026-10-18T09:55:00Z"));
        ReflectionTestUtils.setField(buyer, "id", 12L);
        return buyer;
    }
}
