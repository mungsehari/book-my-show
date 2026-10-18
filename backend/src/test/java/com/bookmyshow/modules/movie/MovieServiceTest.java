package com.bookmyshow.modules.movie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.movie.MovieTypes.CreateMovieRequest;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MovieServiceTest {

    private MovieRepository movieRepository;
    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        movieService = new MovieService(movieRepository);
    }

    @Test
    void listsAllMoviesWhenLanguageBlank() {
        when(movieRepository.findAllByOrderByTitleAsc()).thenReturn(List.of(movie("Sky Riders", "English")));

        List<MovieResponse> result = movieService.listMovies(" ");

        assertThat(result).singleElement().satisfies(r -> assertThat(r.title()).isEqualTo("Sky Riders"));
        verify(movieRepository, never()).findAllByLanguageIgnoreCaseOrderByTitleAsc(" ");
    }

    @Test
    void filtersByTrimmedLanguage() {
        when(movieRepository.findAllByLanguageIgnoreCaseOrderByTitleAsc("hindi"))
                .thenReturn(List.of(movie("The Last Monsoon", "Hindi")));

        List<MovieResponse> result = movieService.listMovies(" hindi ");

        assertThat(result)
                .singleElement()
                .satisfies(r -> assertThat(r.language()).isEqualTo("Hindi"));
    }

    @Test
    void listsMoviesByIds() {
        when(movieRepository.findAllByIdInOrderByTitleAsc(List.of(1L, 2L)))
                .thenReturn(List.of(movie("Sky Riders", "English")));

        assertThat(movieService.listByIds(List.of(1L, 2L)))
                .singleElement()
                .satisfies(r -> assertThat(r.title()).isEqualTo("Sky Riders"));
    }

    @Test
    void skipsRepositoryForEmptyIds() {
        assertThat(movieService.listByIds(List.of())).isEmpty();
        verifyNoInteractions(movieRepository);
    }

    @Test
    void getsMovieById() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie("Sky Riders", "English")));

        assertThat(movieService.getMovie(1L).title()).isEqualTo("Sky Riders");
    }

    @Test
    void failsWhenMovieMissing() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.getMovie(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Movie 99 not found");
    }

    @Test
    void ensureExistsFailsWhenMovieMissing() {
        when(movieRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> movieService.ensureExists(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Movie 99 not found");
    }

    @Test
    void createsMovie() {
        when(movieRepository.save(any(Movie.class))).thenAnswer(call -> call.getArgument(0));

        MovieResponse created = movieService.create(new CreateMovieRequest(
                " Sky Riders ", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null));

        assertThat(created.title()).isEqualTo("Sky Riders");
        assertThat(created.durationMinutes()).isEqualTo(128);
    }

    @Test
    void rejectsDuplicateMovie() {
        when(movieRepository.existsByTitleIgnoreCaseAndLanguageIgnoreCase("Sky Riders", "English"))
                .thenReturn(true);

        assertThatThrownBy(() -> movieService.create(new CreateMovieRequest(
                        "Sky Riders", "Plot.", "English", "Action", 128, "UA", LocalDate.of(2026, 10, 2), null)))
                .isInstanceOf(ConflictException.class);
    }

    private static Movie movie(String title, String language) {
        return new Movie(title, "Sample plot.", language, "Drama", 120, "UA", LocalDate.of(2026, 10, 1), null);
    }
}
