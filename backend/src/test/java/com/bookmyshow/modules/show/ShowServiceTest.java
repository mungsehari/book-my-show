package com.bookmyshow.modules.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.screen.ScreenService;
import com.bookmyshow.modules.show.ShowTypes.CreateShowRequest;
import com.bookmyshow.modules.show.ShowTypes.ShowFilters;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShowServiceTest {

    // 2026-10-17 20:00 UTC is already 2026-10-18 in India.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-17T20:00:00Z"), ZoneId.of("Asia/Kolkata"));

    private static final LocalDate SHOW_DATE = LocalDate.of(2026, 10, 20);
    private static final CurrentUser ADMIN = new CurrentUser(1L, Roles.ADMIN);
    private static final MovieResponse SKY_RIDERS = new MovieResponse(
            2L, "Sky Riders", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null);

    private static final Map<String, BigDecimal> PRICES =
            Map.of("REGULAR", new BigDecimal("200.00"), "PREMIUM", new BigDecimal("300.00"));

    private ShowRepository showRepository;
    private ShowPriceRepository showPriceRepository;
    private CinemaService cinemaService;
    private CinemaAccessService cinemaAccessService;
    private ScreenService screenService;
    private MovieService movieService;
    private ShowService showService;

    @BeforeEach
    void setUp() {
        showRepository = mock(ShowRepository.class);
        showPriceRepository = mock(ShowPriceRepository.class);
        cinemaService = mock(CinemaService.class);
        cinemaAccessService = mock(CinemaAccessService.class);
        screenService = mock(ScreenService.class);
        movieService = mock(MovieService.class);
        when(screenService.listCategories(3L)).thenReturn(Set.of("REGULAR", "PREMIUM"));
        showService = new ShowService(
                showRepository,
                showPriceRepository,
                cinemaService,
                cinemaAccessService,
                screenService,
                movieService,
                CLOCK);
    }

    @Test
    void usesTodayInAppTimeZoneWhenDateMissing() {
        LocalDate today = LocalDate.of(2026, 10, 18);
        Show show = new Show(10L, null, 2L, "Sky Riders", today, LocalTime.of(14, 0), new BigDecimal("280.00"));
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, today))
                .thenReturn(List.of(show));

        List<ShowResponse> result = showService.listByCinema(10L, null, ShowFilters.NONE);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.movieId()).isEqualTo(2L);
            assertThat(r.movieName()).isEqualTo("Sky Riders");
            assertThat(r.showDate()).isEqualTo(today);
        });
        verify(cinemaService).ensureExists(10L);
    }

    @Test
    void filtersQuietSubtitledWheelchairShows() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        Show quietOnScreen3 = show(3L, true, true);
        Show quietOnScreen4 = show(4L, true, true);
        Show loudOnScreen3 = show(3L, false, true);
        Show quietNoSubtitles = show(3L, true, false);
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, date))
                .thenReturn(List.of(quietOnScreen3, quietOnScreen4, loudOnScreen3, quietNoSubtitles));
        when(screenService.withWheelchairSeats(List.of(3L, 4L))).thenReturn(Set.of(3L));

        List<ShowResponse> result = showService.listByCinema(10L, date, new ShowFilters(true, false, true, true));

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.screenId()).isEqualTo(3L);
            assertThat(r.quiet()).isTrue();
            assertThat(r.subtitles()).isTrue();
        });
    }

    @Test
    void createsShowWithFeatureFlags() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        when(showRepository.save(any(Show.class))).thenAnswer(call -> call.getArgument(0));

        ShowResponse created = showService.create(
                3L, new CreateShowRequest(2L, SHOW_DATE, LocalTime.of(14, 0), PRICES, true, null, true), ADMIN);

        assertThat(created.quiet()).isTrue();
        assertThat(created.sensoryFriendly()).isFalse();
        assertThat(created.subtitles()).isTrue();
    }

    private static Show show(long screenId, boolean quiet, boolean subtitles) {
        Show show = new Show(
                10L, screenId, 2L, "Sky Riders", LocalDate.of(2026, 10, 20), LocalTime.of(14, 0), BigDecimal.TEN);
        show.setFeatures(quiet, false, subtitles);
        return show;
    }

    @Test
    void usesGivenDate() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(10L, date))
                .thenReturn(List.of());

        assertThat(showService.listByCinema(10L, date, ShowFilters.NONE)).isEmpty();
    }

    @Test
    void failsWhenCinemaMissing() {
        doThrow(new NotFoundException("Cinema 99 not found"))
                .when(cinemaService)
                .ensureExists(99L);

        assertThatThrownBy(() -> showService.listByCinema(99L, null, ShowFilters.NONE))
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
    void createsShowOnScreen() {
        CurrentUser user = new CurrentUser(2L, Roles.THEATRE_ADMIN);
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        when(showRepository.save(any(Show.class))).thenAnswer(call -> call.getArgument(0));

        ShowResponse created = showService.create(3L, request(LocalTime.of(14, 0)), user);

        verify(cinemaAccessService).ensureCanManage(user, 10L);
        assertThat(created.cinemaId()).isEqualTo(10L);
        assertThat(created.screenId()).isEqualTo(3L);
        assertThat(created.movieName()).isEqualTo("Sky Riders");
        assertThat(created.price()).isEqualByComparingTo("200.00");
        ArgumentCaptor<List<ShowPrice>> prices = ArgumentCaptor.captor();
        verify(showPriceRepository).saveAll(prices.capture());
        assertThat(prices.getValue())
                .extracting(ShowPrice::getCategory)
                .containsExactlyInAnyOrder("REGULAR", "PREMIUM");
    }

    @Test
    void rejectsMissingCategoryPrice() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        CreateShowRequest onlyRegular = new CreateShowRequest(
                2L, SHOW_DATE, LocalTime.of(14, 0), Map.of("REGULAR", new BigDecimal("200.00")), null, null, null);

        assertThatThrownBy(() -> showService.create(3L, onlyRegular, ADMIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Prices must cover exactly the seat categories on screen 3: [PREMIUM, REGULAR]");
        verify(showRepository, never()).save(any());
    }

    @Test
    void rejectsPriceForCategoryNotOnScreen() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        CreateShowRequest extra = new CreateShowRequest(
                2L,
                SHOW_DATE,
                LocalTime.of(14, 0),
                Map.of(
                        "REGULAR", new BigDecimal("200.00"),
                        "PREMIUM", new BigDecimal("300.00"),
                        "RECLINER", new BigDecimal("500.00")),
                null,
                null,
                null);

        assertThatThrownBy(() -> showService.create(3L, extra, ADMIN)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsOverlappingShow() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        // Existing 12:00 show of a 128 min movie blocks the screen until 14:23 (with 15 min cleaning).
        when(showRepository.findAllByScreenIdAndShowDate(3L, SHOW_DATE))
                .thenReturn(List.of(
                        new Show(10L, 3L, 2L, "Sky Riders", SHOW_DATE, LocalTime.of(12, 0), new BigDecimal("250.00"))));
        when(movieService.listByIds(List.of(2L))).thenReturn(List.of(SKY_RIDERS));

        assertThatThrownBy(() -> showService.create(3L, request(LocalTime.of(14, 0)), ADMIN))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("12:00 to 14:23");
        verify(showRepository, never()).save(any());
    }

    @Test
    void allowsShowRightAfterCleaning() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        when(showRepository.findAllByScreenIdAndShowDate(3L, SHOW_DATE))
                .thenReturn(List.of(
                        new Show(10L, 3L, 2L, "Sky Riders", SHOW_DATE, LocalTime.of(12, 0), new BigDecimal("250.00"))));
        when(movieService.listByIds(List.of(2L))).thenReturn(List.of(SKY_RIDERS));
        when(showRepository.save(any(Show.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(showService.create(3L, request(LocalTime.of(14, 23)), ADMIN).startTime())
                .isEqualTo(LocalTime.of(14, 23));
    }

    @Test
    void rejectsShowInThePast() {
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        when(movieService.getMovie(2L)).thenReturn(SKY_RIDERS);
        // The clock is 2026-10-18 01:30 in India.
        CreateShowRequest past =
                new CreateShowRequest(2L, LocalDate.of(2026, 10, 18), LocalTime.of(1, 0), PRICES, null, null, null);

        assertThatThrownBy(() -> showService.create(3L, past, ADMIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Show must start in the future");
    }

    @Test
    void stopsWhenUserCannotManageCinema() {
        CurrentUser user = new CurrentUser(2L, Roles.THEATRE_ADMIN);
        when(screenService.lockAndGetCinemaId(3L)).thenReturn(10L);
        doThrow(new ForbiddenException("You cannot manage cinema 10"))
                .when(cinemaAccessService)
                .ensureCanManage(user, 10L);

        assertThatThrownBy(() -> showService.create(3L, request(LocalTime.of(14, 0)), user))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(movieService);
    }

    private static CreateShowRequest request(LocalTime startTime) {
        return new CreateShowRequest(2L, SHOW_DATE, startTime, PRICES, null, null, null);
    }

    @Test
    void failsWhenCityMissing() {
        when(cinemaService.listIdsByCity(99L)).thenThrow(new NotFoundException("City 99 not found"));

        assertThatThrownBy(() -> showService.listMoviesInCity(99L, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("City 99 not found");
    }
}
