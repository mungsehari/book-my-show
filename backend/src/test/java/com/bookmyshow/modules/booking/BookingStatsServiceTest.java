package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.SaleFact;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class BookingStatsServiceTest {

    private static final Instant FROM = Instant.parse("2026-10-12T00:00:00Z");

    private BookingRepository bookingRepository;
    private BookingStatsService service;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        service = new BookingStatsService(bookingRepository);
    }

    @Test
    void salesCarryAmountMovieAndSeatCount() {
        Booking booking = BookingServiceTest.pending();
        booking.confirm("pay_1", Instant.parse("2026-10-17T05:00:00Z"));
        when(bookingRepository.findByStatusAndConfirmedAtGreaterThanEqualAndCinemaIdIn(
                        BookingStatus.CONFIRMED, FROM, List.of(10L)))
                .thenReturn(List.of(booking));

        List<SaleFact> sales = service.sales(List.of(10L), FROM);

        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(sales.get(0).movieName()).isEqualTo("Sky Riders");
        assertThat(sales.get(0).tickets()).isEqualTo(1);
    }

    @Test
    void noCinemasMeansNoDataAndNoQueries() {
        assertThat(service.sales(List.of(), FROM)).isEmpty();
        assertThat(service.cancelledSince(List.of(), FROM)).isZero();
        assertThat(service.refundsPending(List.of())).isZero();
        assertThat(service.list(List.of(), null, 0, 20).items()).isEmpty();

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void listWithoutStatusReadsAllBookings() {
        Booking booking = BookingServiceTest.pending();
        when(bookingRepository.findByCinemaIdIn(any(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(booking), call.getArgument(1), 1));

        PageResponse<BookingResponse> page = service.list(List.of(10L), null, 0, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void listWithStatusFiltersByIt() {
        Booking booking = BookingServiceTest.pending();
        when(bookingRepository.findByStatusAndCinemaIdIn(
                        org.mockito.ArgumentMatchers.eq(BookingStatus.NEEDS_REFUND), any(), any(Pageable.class)))
                .thenAnswer(call -> new PageImpl<>(List.of(booking), call.getArgument(2), 1));

        assertThat(service.list(List.of(10L), BookingStatus.NEEDS_REFUND, 0, 20).items())
                .hasSize(1);
    }
}
