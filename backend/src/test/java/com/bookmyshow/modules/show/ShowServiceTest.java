package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
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
    private MovieService movieService;
    private ShowService showService;

    @BeforeEach
    void setUp() {
        showRepository = mock(ShowRepository.class);
        cinemaService = mock(CinemaService.class);
        movieService = mock(MovieService.class);
        showService = new ShowService(showRepository, cinemaService, movieService, CLOCK);
    }

    @Test
    void usesTodayInAppTimeZoneWhenDateMissing() {
        LocalDate today = LocalDate.of(2026, 10, 18);
        Show show = new Show(10L, 2L, "Sky Riders", today, LocalTime.of(14, 0), new BigDecimal("280.00"));
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, today))
                .thenReturn(List.of(show));

        List<ShowResponse> result = showService.listByCinema(10L, null);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.movieId()).isEqualTo(2L);
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

    @Test
    void listsMoviesInCityForNextSevenDaysWhenDateMissing() {
        LocalDate today = LocalDate.of(2026, 10, 18);
        MovieResponse movie = new MovieResponse(
                2L, "Sky Riders", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null);
        when(cinemaService.listIdsByCity(1L)).thenReturn(List.of(10L, 11L));
        when(showRepository.findMovieIdsPlaying(List.of(10L, 11L), today, today.plusDays(6)))
                .thenReturn(List.of(2L));
        when(movieService.listByIds(List.of(2L))).thenReturn(List.of(movie));

        assertThat(showService.listMoviesInCity(1L, null)).containsExactly(movie);
    }

    @Test
    void listsMoviesInCityForGivenDateOnly() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(cinemaService.listIdsByCity(1L)).thenReturn(List.of(10L));
        when(showRepository.findMovieIdsPlaying(List.of(10L), date, date)).thenReturn(List.of());
        when(movieService.listByIds(List.of())).thenReturn(List.of());

        assertThat(showService.listMoviesInCity(1L, date)).isEmpty();
        verify(showRepository).findMovieIdsPlaying(List.of(10L), date, date);
    }

    @Test
    void returnsNoMoviesWhenCityHasNoCinemas() {
        when(cinemaService.listIdsByCity(1L)).thenReturn(List.of());

        assertThat(showService.listMoviesInCity(1L, null)).isEmpty();
        verifyNoInteractions(showRepository, movieService);
    }

    @Test
    void failsWhenCityMissing() {
        when(cinemaService.listIdsByCity(99L)).thenThrow(new NotFoundException("City 99 not found"));

        assertThatThrownBy(() -> showService.listMoviesInCity(99L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("City 99 not found");
    }
}
