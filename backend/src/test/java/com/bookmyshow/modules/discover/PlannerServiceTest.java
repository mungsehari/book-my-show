package com.bookmyshow.modules.discover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.cinema.CinemaPlaceService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaResponse;
import com.bookmyshow.modules.cinema.CinemaTypes.PlaceResponse;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanRequest;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanResponse;
import com.bookmyshow.modules.discover.QueryInterpreter.Interpretation;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowSeatMapService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlannerServiceTest {

    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 24);

    private QueryInterpreter interpreter;
    private ShowService showService;
    private ShowCrowdService crowdService;
    private ShowSeatMapService seatMapService;
    private PlannerService planner;

    @BeforeEach
    void setUp() {
        interpreter = mock(QueryInterpreter.class);
        showService = mock(ShowService.class);
        crowdService = mock(ShowCrowdService.class);
        seatMapService = mock(ShowSeatMapService.class);
        MovieService movieService = mock(MovieService.class);
        CinemaService cinemaService = mock(CinemaService.class);
        CinemaPlaceService placeService = mock(CinemaPlaceService.class);
        when(movieService.listByIds(anyList()))
                .thenReturn(List.of(movie(2L, "Comedy", 120), movie(3L, "Thriller", 150)));
        when(cinemaService.getCinema(10L)).thenReturn(new CinemaResponse(10L, 1L, "City Cineplex", "Andheri"));
        when(placeService.list(10L))
                .thenReturn(List.of(
                        new PlaceResponse(1L, 10L, "PARKING", "Mall parking", 2, "Rs 50"),
                        new PlaceResponse(2L, 10L, "FOOD", "Chai Point", 3, null)));
        planner = new PlannerService(
                interpreter,
                showService,
                crowdService,
                seatMapService,
                movieService,
                cinemaService,
                placeService,
                Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void cheapestMatchingEveningShowsWithFoodAndParking() {
        when(interpreter.interpret("kuch funny", "ip"))
                .thenReturn(new Interpretation(new MovieFilters(Set.of("Comedy"), Set.of(), null, Set.of()), "claude"));
        when(showService.listInCityOnDate(1L, SATURDAY))
                .thenReturn(List.of(
                        show(50L, 2L, 14), // afternoon: out of slot
                        show(51L, 2L, 18),
                        show(52L, 2L, 21),
                        show(53L, 3L, 19))); // thriller: wrong mood
        when(crowdService.getCrowd(anyLong())).thenReturn(new CrowdResponse(0L, 100, 0, 0, 100, 0, "LOW"));
        when(seatMapService.lowestAvailablePrice(51L)).thenReturn(Optional.of(new BigDecimal("300.00")));
        when(seatMapService.lowestAvailablePrice(52L)).thenReturn(Optional.of(new BigDecimal("200.00")));

        PlanResponse plan = planner.plan(request("kuch funny", null), "ip");

        assertThat(plan.source()).isEqualTo("claude");
        assertThat(plan.plans()).extracting("showId").containsExactly(52L, 51L);
        assertThat(plan.plans().get(0).totalPrice()).isEqualByComparingTo("400.00");
        assertThat(plan.plans().get(0).food().name()).isEqualTo("Chai Point");
        assertThat(plan.plans().get(0).parking().name()).isEqualTo("Mall parking");
        verify(seatMapService, never()).lowestAvailablePrice(53L);
    }

    @Test
    void skipsShowsWithoutEnoughSeatsOrOverBudget() {
        when(showService.listInCityOnDate(1L, SATURDAY)).thenReturn(List.of(show(51L, 2L, 18), show(52L, 2L, 21)));
        when(crowdService.getCrowd(51L)).thenReturn(new CrowdResponse(51L, 100, 99, 0, 1, 99, "HIGH"));
        when(crowdService.getCrowd(52L)).thenReturn(new CrowdResponse(52L, 100, 0, 0, 100, 0, "LOW"));
        when(seatMapService.lowestAvailablePrice(52L)).thenReturn(Optional.of(new BigDecimal("350.00")));

        PlanResponse plan = planner.plan(request(null, new BigDecimal("300.00")), "ip");

        assertThat(plan.plans()).isEmpty();
        assertThat(plan.source()).isEqualTo("none");
        verify(interpreter, never()).interpret(anyString(), anyString());
    }

    @Test
    void rejectsPastDate() {
        PlanRequest past = new PlanRequest(1L, LocalDate.of(2026, 10, 1), null, 2, null, "EVENING");

        assertThatThrownBy(() -> planner.plan(past, "ip")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void timeSlots() {
        assertThat(PlannerService.inSlot(LocalTime.of(11, 59), "MORNING")).isTrue();
        assertThat(PlannerService.inSlot(LocalTime.NOON, "AFTERNOON")).isTrue();
        assertThat(PlannerService.inSlot(LocalTime.of(17, 0), "AFTERNOON")).isFalse();
        assertThat(PlannerService.inSlot(LocalTime.of(17, 0), "EVENING")).isTrue();
    }

    private static PlanRequest request(String mood, BigDecimal budget) {
        return new PlanRequest(1L, SATURDAY, mood, 2, budget, "EVENING");
    }

    private static ShowResponse show(long id, long movieId, int hour) {
        return new ShowResponse(
                id,
                10L,
                3L,
                movieId,
                movieId == 2L ? "Sky Riders" : "Code Red",
                SATURDAY,
                LocalTime.of(hour, 0),
                new BigDecimal("200.00"),
                false,
                false,
                false);
    }

    private static MovieResponse movie(long id, String genre, int minutes) {
        return new MovieResponse(id, "Movie " + id, "Plot.", "Hindi", genre, minutes, "UA", SATURDAY, null);
    }
}
