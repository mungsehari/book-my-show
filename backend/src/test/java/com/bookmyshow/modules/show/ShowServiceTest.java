package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowServiceTest {

    // 2026-10-17 20:00 UTC is already 2026-10-18 in India.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T20:00:00Z"), ZoneId.of("Asia/Kolkata"));

    private ShowRepository showRepository;
    private CinemaService cinemaService;
    private ShowService showService;

    @BeforeEach
    void setUp() {
        showRepository = mock(ShowRepository.class);
        cinemaService = mock(CinemaService.class);
        showService = new ShowService(showRepository, cinemaService, CLOCK);
    }

    @Test
    void usesTodayInAppTimeZoneWhenDateMissing() {
        LocalDate today = LocalDate.of(2026, 10, 18);
        Show show = new Show(10L, "Sky Riders", today, LocalTime.of(14, 0), new BigDecimal("280.00"));
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, today))
                .thenReturn(List.of(show));

        List<ShowResponse> result = showService.listByCinema(10L, null);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.movieName()).isEqualTo("Sky Riders");
            assertThat(r.showDate()).isEqualTo(today);
        });
        verify(cinemaService).ensureExists(10L);
    }

    @Test
    void usesGivenDate() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, date))
                .thenReturn(List.of());

        assertThat(showService.listByCinema(10L, date)).isEmpty();
    }

    @Test
    void failsWhenCinemaMissing() {
        doThrow(new NotFoundException("Cinema 99 not found"))
                .when(cinemaService)
                .ensureExists(99L);

        assertThatThrownBy(() -> showService.listByCinema(99L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Cinema 99 not found");
    }
}
