package com.bookmyshow.modules.discover;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.cinema.CinemaPlaceService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.cinema.CinemaTypes.PlaceResponse;
import com.bookmyshow.modules.discover.DiscoverTypes.PlaceSummary;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanOption;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanRequest;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanResponse;
import com.bookmyshow.modules.discover.QueryInterpreter.Interpretation;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowSeatMapService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weekend planner: a movie, food, and parking in one answer. Claude only reads the mood; the plans themselves are built
 * from live shows, seats, and prices, so every plan is real and bookable now.
 */
@Service
@Transactional(readOnly = true)
public class PlannerService {

    /** At most this many shows are checked for seats and prices per request, to keep it quick. */
    static final int MAX_CANDIDATES = 20;

    static final int MAX_PLANS = 3;

    private final QueryInterpreter interpreter;
    private final ShowService showService;
    private final ShowCrowdService crowdService;
    private final ShowSeatMapService seatMapService;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final CinemaPlaceService placeService;
    private final Clock clock;

    public PlannerService(
            QueryInterpreter interpreter,
            ShowService showService,
            ShowCrowdService crowdService,
            ShowSeatMapService seatMapService,
            MovieService movieService,
            CinemaService cinemaService,
            CinemaPlaceService placeService,
            Clock clock) {
        this.interpreter = interpreter;
        this.showService = showService;
        this.crowdService = crowdService;
        this.seatMapService = seatMapService;
        this.movieService = movieService;
        this.cinemaService = cinemaService;
        this.placeService = placeService;
        this.clock = clock;
    }

    public PlanResponse plan(PlanRequest request, String clientKey) {
        if (request.date().isBefore(LocalDate.now(clock))) {
            throw new BadRequestException("Pick today or a later date");
        }
        Interpretation interpretation = request.mood() == null || request.mood().isBlank()
                ? new Interpretation(MovieFilters.NONE, "none")
                : interpreter.interpret(request.mood(), clientKey);

        List<ShowResponse> shows = showService.listInCityOnDate(request.cityId(), request.date()).stream()
                .filter(show -> inSlot(show.startTime(), request.time()))
                .toList();
        Map<Long, MovieResponse> movies = movieService
                .listByIds(shows.stream()
                        .map(ShowResponse::movieId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(MovieResponse::id, Function.identity()));

        List<PlanOption> options = new ArrayList<>();
        Map<Long, String> cinemaNames = new HashMap<>();
        shows.stream()
                .filter(show -> show.movieId() != null && movies.containsKey(show.movieId()))
                .filter(show -> MoodSearchService.matches(movies.get(show.movieId()), interpretation.filters()))
                .limit(MAX_CANDIDATES)
                .forEach(show -> option(show, request, cinemaNames).ifPresent(options::add));

        List<PlanOption> best = options.stream()
                .sorted(Comparator.comparing(PlanOption::totalPrice).thenComparing(PlanOption::startTime))
                .limit(MAX_PLANS)
                .toList();
        return new PlanResponse(request.date(), interpretation.filters(), interpretation.source(), best);
    }

    private Optional<PlanOption> option(ShowResponse show, PlanRequest request, Map<Long, String> cinemaNames) {
        int available = crowdService.getCrowd(show.id()).available();
        if (available < request.people()) {
            return Optional.empty();
        }
        Optional<BigDecimal> lowest = seatMapService.lowestAvailablePrice(show.id());
        if (lowest.isEmpty()
                || (request.budgetPerPerson() != null && lowest.get().compareTo(request.budgetPerPerson()) > 0)) {
            return Optional.empty();
        }
        String cinemaName = cinemaNames.computeIfAbsent(
                show.cinemaId(), id -> cinemaService.getCinema(id).name());
        List<PlaceResponse> places = placeService.list(show.cinemaId());
        return Optional.of(new PlanOption(
                show.id(),
                show.movieId(),
                show.movieName(),
                show.cinemaId(),
                cinemaName,
                show.startTime(),
                available,
                lowest.get(),
                lowest.get().multiply(BigDecimal.valueOf(request.people())),
                nearest(places, "FOOD"),
                nearest(places, "PARKING")));
    }

    /** MORNING is before 12, AFTERNOON 12 to 5 PM, EVENING from 5 PM. */
    static boolean inSlot(LocalTime start, String slot) {
        return switch (slot) {
            case "MORNING" -> start.isBefore(LocalTime.NOON);
            case "AFTERNOON" -> !start.isBefore(LocalTime.NOON) && start.isBefore(LocalTime.of(17, 0));
            default -> !start.isBefore(LocalTime.of(17, 0));
        };
    }

    private static PlaceSummary nearest(List<PlaceResponse> places, String type) {
        return places.stream()
                .filter(place -> type.equals(place.type()))
                .findFirst()
                .map(place -> new PlaceSummary(place.name(), place.walkMinutes(), place.notes()))
                .orElse(null);
    }
}
