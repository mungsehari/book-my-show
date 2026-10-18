package com.bookmyshow.modules.discover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.discover.DiscoverTypes.MoodSearchResponse;
import com.bookmyshow.modules.discover.QueryInterpreter.Interpretation;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MoodSearchServiceTest {

    private static final MovieResponse SHORT_COMEDY = movie(1L, "Comedy", 110, "Hindi", "U");
    private static final MovieResponse LONG_COMEDY = movie(2L, "Comedy", 150, "Hindi", "UA");
    private static final MovieResponse THRILLER = movie(3L, "Thriller", 100, "English", "A");

    @Test
    void keepsOnlyMatchingMovies() {
        QueryInterpreter interpreter = mock(QueryInterpreter.class);
        MovieService movieService = mock(MovieService.class);
        when(interpreter.interpret("kuch halka funny 2 ghante se kam", "ip"))
                .thenReturn(new Interpretation(new MovieFilters(Set.of("comedy"), Set.of(), 120, Set.of()), "keyword"));
        when(movieService.listMovies(null)).thenReturn(List.of(SHORT_COMEDY, LONG_COMEDY, THRILLER));

        MoodSearchResponse response = new MoodSearchService(interpreter, movieService, mock(ShowService.class))
                .search("kuch halka funny 2 ghante se kam", null, "ip");

        assertThat(response.movies()).containsExactly(SHORT_COMEDY);
        assertThat(response.source()).isEqualTo("keyword");
    }

    @Test
    void cityLimitsToMoviesPlayingThere() {
        QueryInterpreter interpreter = mock(QueryInterpreter.class);
        ShowService showService = mock(ShowService.class);
        when(interpreter.interpret("anything", "ip")).thenReturn(new Interpretation(MovieFilters.NONE, "keyword"));
        when(showService.listMoviesInCity(1L, null)).thenReturn(List.of(THRILLER));

        MoodSearchResponse response =
                new MoodSearchService(interpreter, mock(MovieService.class), showService).search("anything", 1L, "ip");

        assertThat(response.movies()).containsExactly(THRILLER);
    }

    @Test
    void certificateAndLanguageFilters() {
        MovieFilters kidsHindi = new MovieFilters(Set.of(), Set.of("Hindi"), null, Set.of("U"));

        assertThat(MoodSearchService.matches(SHORT_COMEDY, kidsHindi)).isTrue();
        assertThat(MoodSearchService.matches(LONG_COMEDY, kidsHindi)).isFalse();
        assertThat(MoodSearchService.matches(THRILLER, kidsHindi)).isFalse();
    }

    private static MovieResponse movie(long id, String genre, int minutes, String language, String certificate) {
        return new MovieResponse(
                id, "Movie " + id, "Plot.", language, genre, minutes, certificate, LocalDate.of(2026, 10, 1), null);
    }
}
