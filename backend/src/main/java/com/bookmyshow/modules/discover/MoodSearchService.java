package com.bookmyshow.modules.discover;

import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.discover.DiscoverTypes.MoodSearchResponse;
import com.bookmyshow.modules.discover.QueryInterpreter.Interpretation;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowService;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** "Kuch halka funny, 2 ghante se kam": read the request into filters, then match movies from our catalog. */
@Service
@Transactional(readOnly = true)
public class MoodSearchService {

    private final QueryInterpreter interpreter;
    private final MovieService movieService;
    private final ShowService showService;

    public MoodSearchService(QueryInterpreter interpreter, MovieService movieService, ShowService showService) {
        this.interpreter = interpreter;
        this.movieService = movieService;
        this.showService = showService;
    }

    /** With a city, only movies playing there in the next 7 days. */
    public MoodSearchResponse search(String query, Long cityId, String clientKey) {
        Interpretation interpretation = interpreter.interpret(query, clientKey);
        List<MovieResponse> pool =
                cityId == null ? movieService.listMovies(null) : showService.listMoviesInCity(cityId, null);
        List<MovieResponse> movies = pool.stream()
                .filter(movie -> matches(movie, interpretation.filters()))
                .toList();
        return new MoodSearchResponse(query, interpretation.filters(), interpretation.source(), movies);
    }

    static boolean matches(MovieResponse movie, MovieFilters filters) {
        return inSet(movie.genre(), filters.genres())
                && inSet(movie.language(), filters.languages())
                && inSet(movie.certificate(), filters.certificates())
                && (filters.maxDurationMinutes() == null || movie.durationMinutes() <= filters.maxDurationMinutes());
    }

    private static boolean inSet(String value, Set<String> allowed) {
        if (allowed.isEmpty()) {
            return true;
        }
        Set<String> lower =
                allowed.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return value != null && lower.contains(value.toLowerCase(Locale.ROOT));
    }
}
