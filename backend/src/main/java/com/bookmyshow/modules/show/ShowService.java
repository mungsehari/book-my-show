package com.bookmyshow.modules.show;

import com.bookmyshow.modules.cinema.CinemaService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShowService {

    /** Days covered by "now showing" when no date is given, today included. */
    static final int NOW_SHOWING_DAYS = 7;

    private final ShowRepository showRepository;
    private final CinemaService cinemaService;
    private final MovieService movieService;
    private final Clock clock;

    public ShowService(
            ShowRepository showRepository, CinemaService cinemaService, MovieService movieService, Clock clock) {
        this.showRepository = showRepository;
        this.cinemaService = cinemaService;
        this.movieService = movieService;
        this.clock = clock;
    }

    /** Lists shows for a cinema on a date. No date means today in the app time zone. */
    public List<ShowResponse> listByCinema(long cinemaId, LocalDate date) {
        cinemaService.ensureExists(cinemaId);
        LocalDate showDate = date != null ? date : LocalDate.now(clock);
        return showRepository.findAllByCinemaIdAndShowDateOrderByStartTimeAsc(cinemaId, showDate).stream()
                .map(ShowResponse::from)
                .toList();
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
}
