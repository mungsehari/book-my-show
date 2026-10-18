package com.bookmyshow.modules.movie;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.movie.MovieTypes.CreateMovieRequest;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /** Lists movies by title. A blank or missing language means all languages. */
    public List<MovieResponse> listMovies(String language) {
        List<Movie> movies = language == null || language.isBlank()
                ? movieRepository.findAllByOrderByTitleAsc()
                : movieRepository.findAllByLanguageIgnoreCaseOrderByTitleAsc(language.strip());
        return movies.stream().map(MovieResponse::from).toList();
    }

    /** Used by other modules to load movies by ID, sorted by title. Unknown IDs are skipped. */
    public List<MovieResponse> listByIds(Collection<Long> movieIds) {
        if (movieIds.isEmpty()) {
            return List.of();
        }
        return movieRepository.findAllByIdInOrderByTitleAsc(movieIds).stream()
                .map(MovieResponse::from)
                .toList();
    }

    @Transactional
    public MovieResponse create(CreateMovieRequest request) {
        String title = request.title().strip();
        String language = request.language().strip();
        if (movieRepository.existsByTitleIgnoreCaseAndLanguageIgnoreCase(title, language)) {
            throw new ConflictException("Movie '" + title + "' in " + language + " already exists");
        }
        Movie movie = movieRepository.save(new Movie(
                title,
                request.description().strip(),
                language,
                request.genre().strip(),
                request.durationMinutes(),
                request.certificate(),
                request.releaseDate(),
                request.posterUrl()));
        return MovieResponse.from(movie);
    }

    public MovieResponse getMovie(long movieId) {
        return movieRepository.findById(movieId).map(MovieResponse::from).orElseThrow(() -> notFound(movieId));
    }

    /** Used by other modules to check a movie before using its ID. */
    public void ensureExists(long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw notFound(movieId);
        }
    }

    private static NotFoundException notFound(long movieId) {
        return new NotFoundException("Movie " + movieId + " not found");
    }
}
