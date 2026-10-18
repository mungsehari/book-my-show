package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShowSeatBookingServiceTest {

    private ShowRepository showRepository;
    private ShowBookedSeatRepository bookedSeatRepository;
    private ShowSeatBookingService service;

    @BeforeEach
    void setUp() {
        showRepository = mock(ShowRepository.class);
        bookedSeatRepository = mock(ShowBookedSeatRepository.class);
        service = new ShowSeatBookingService(
                showRepository,
                bookedSeatRepository,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void sellsFreeSeatsAfterLockingTheShow() {
        when(showRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(show()));
        when(bookedSeatRepository.existsByShowIdAndSeatIdIn(50L, List.of(1L, 2L)))
                .thenReturn(false);

        assertThat(service.sellSeats(50L, List.of(1L, 2L), 9L)).isTrue();

        ArgumentCaptor<List<ShowBookedSeat>> saved = ArgumentCaptor.captor();
        verify(bookedSeatRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ShowBookedSeat::getSeatId).containsExactly(1L, 2L);
    }

    @Test
    void sellsNothingIfAnySeatIsSold() {
        when(showRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(show()));
        when(bookedSeatRepository.existsByShowIdAndSeatIdIn(50L, List.of(1L, 2L)))
                .thenReturn(true);

        assertThat(service.sellSeats(50L, List.of(1L, 2L), 9L)).isFalse();
        verify(bookedSeatRepository, never()).saveAll(anyList());
    }

    @Test
    void failsForMissingShow() {
        when(showRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sellSeats(99L, List.of(1L), 9L)).isInstanceOf(NotFoundException.class);
    }

    private static Show show() {
        return new Show(
                10L, 3L, 2L, "Sky Riders", LocalDate.of(2026, 10, 20), LocalTime.of(14, 0), new BigDecimal("200.00"));
    }
}
