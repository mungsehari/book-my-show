package com.bookmyshow.modules.show;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.screen.ScreenService;
import com.bookmyshow.modules.show.ShowTypes.CreateShowRequest;
import com.bookmyshow.modules.show.ShowTypes.ShowFilters;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShowService {

    /** Days covered by "now showing" when no date is given, today included. */
    static final int NOW_SHOWING_DAYS = 7;

    /** Time to clean the screen between two shows. */
    static final Duration CLEANING_BUFFER = Duration.ofMinutes(15);

    /** Used for old shows that are not linked to a movie, so their length is unknown. */
    static final Duration UNKNOWN_LENGTH = Duration.ofMinutes(180);

    private final ShowRepository showRepository;
    private final ShowPriceRepository showPriceRepository;
    private final CinemaService cinemaService;
    private final CinemaAccessService cinemaAccessService;
    private final ScreenService screenService;
    private final MovieService movieService;
    private final Clock clock;

    public ShowService(
            ShowRepository showRepository,
            ShowPriceRepository showPriceRepository,
            CinemaService cinemaService,
            CinemaAccessService cinemaAccessService,
            ScreenService screenService,
            MovieService movieService,
            Clock clock) {
        this.showRepository = showRepository;
        this.showPriceRepository = showPriceRepository;
        this.cinemaService = cinemaService;
        this.cinemaAccessService = cinemaAccessService;
        this.screenService = screenService;
        this.movieService = movieService;
        this.clock = clock;
    }

    /**
     * Lists shows for a cinema on a date. No date means today in the app time zone. Filters keep only quiet,
     * sensory-friendly, subtitled, or wheelchair-ready shows when asked.
     */
    public List<ShowResponse> listByCinema(long cinemaId, LocalDate date, ShowFilters filters) {
        cinemaService.ensureExists(cinemaId);
        LocalDate showDate = date != null ? date : LocalDate.now(clock);
        List<Show> shows = showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(cinemaId, showDate).stream()
                .filter(show -> !filters.quiet() || show.isQuiet())
                .filter(show -> !filters.sensoryFriendly() || show.isSensoryFriendly())
                .filter(show -> !filters.subtitles() || show.hasSubtitles())
                .toList();
        if (filters.wheelchair()) {
            Set<Long> ready = screenService.withWheelchairSeats(shows.stream()
                    .map(Show::getScreenId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
            shows = shows.stream()
                    .filter(show -> ready.contains(show.getScreenId()))
                    .toList();
        }
        return shows.stream().map(ShowResponse::from).toList();
    }

    /** Lists movies playing in a city on a date. No date means the next 7 days, today included. */
    public List<MovieResponse> listMoviesInCity(long cityId, LocalDate date) {
        List<Long> cinemaIds = cinemaService.listIdsByCity(cityId);
        if (cinemaIds.isEmpty()) {
            return List.of();
        }
        LocalDate from = date != null ? date : LocalDate.now(clock);
        LocalDate to = date != null ? date : from.plusDays(NOW_SHOWING_DAYS - 1);
        return movieService.listByIds(showRepository.findMovieIdsPlaying(cinemaIds, from, to));
    }

    /**
     * Adds a show to a screen. The screen row is locked first, so two admins cannot add overlapping shows to the same
     * screen at the same time. A show blocks the screen for the movie length plus a cleaning buffer.
     */
    @Transactional
    public ShowResponse create(long screenId, CreateShowRequest request, CurrentUser user) {
        long cinemaId = screenService.lockAndGetCinemaId(screenId);
        cinemaAccessService.ensureCanManage(user, cinemaId);
        MovieResponse movie = movieService.getMovie(request.movieId());

        LocalDateTime start = LocalDateTime.of(request.showDate(), request.startTime());
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("Show must start in the future");
        }
        ensurePricesMatchScreen(screenId, request.prices().keySet());
        LocalDateTime end = start.plus(blockedTime(movie.durationMinutes()));
        ensureNoOverlap(screenId, request.showDate(), start, end);

        BigDecimal lowest = Collections.min(request.prices().values());
        Show newShow = new Show(
                cinemaId, screenId, movie.id(), movie.title(), request.showDate(), request.startTime(), lowest);
        newShow.setFeatures(
                Boolean.TRUE.equals(request.quiet()),
                Boolean.TRUE.equals(request.sensoryFriendly()),
                Boolean.TRUE.equals(request.subtitles()));
        newShow.setDealsEnabled(!Boolean.FALSE.equals(request.lastMinuteDeals()));
        Show show = showRepository.save(newShow);
        showPriceRepository.saveAll(request.prices().entrySet().stream()
                .map(entry -> new ShowPrice(show.getId(), entry.getKey(), entry.getValue()))
                .toList());
        return ShowResponse.from(show);
    }

    /** Every seat category on the screen needs a price, and no price may name a category the screen lacks. */
    private void ensurePricesMatchScreen(long screenId, Set<String> priced) {
        Set<String> onScreen = screenService.listCategories(screenId);
        if (onScreen.isEmpty()) {
            throw new BadRequestException("Screen " + screenId + " has no seats");
        }
        if (!priced.equals(onScreen)) {
            throw new BadRequestException("Prices must cover exactly the seat categories on screen " + screenId + ": "
                    + new TreeSet<>(onScreen));
        }
    }

    private void ensureNoOverlap(long screenId, LocalDate date, LocalDateTime start, LocalDateTime end) {
        List<Show> existing = showRepository.findAllByScreenIdAndShowDate(screenId, date);
        if (existing.isEmpty()) {
            return;
        }
        List<Long> movieIds =
                existing.stream().map(Show::getMovieId).filter(Objects::nonNull).toList();
        Map<Long, Integer> lengths = movieService.listByIds(movieIds).stream()
                .collect(Collectors.toMap(MovieResponse::id, MovieResponse::durationMinutes));

        for (Show other : existing) {
            LocalDateTime otherStart = LocalDateTime.of(other.getShowDate(), other.getStartTime());
            Integer minutes = other.getMovieId() == null ? null : lengths.get(other.getMovieId());
            LocalDateTime otherEnd =
                    otherStart.plus(minutes == null ? UNKNOWN_LENGTH.plus(CLEANING_BUFFER) : blockedTime(minutes));
            if (start.isBefore(otherEnd) && otherStart.isBefore(end)) {
                throw new ConflictException("Screen " + screenId + " already has a show from " + other.getStartTime()
                        + " to " + otherEnd.toLocalTime() + " on " + date);
            }
        }
    }

    private static Duration blockedTime(int movieMinutes) {
        return Duration.ofMinutes(movieMinutes).plus(CLEANING_BUFFER);
    }
}
