package com.bookmyshow.modules.movie;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    @Operation(summary = "List movies, optionally by language")
    public ListResponse<MovieResponse> listMovies(
            @Parameter(description = "Language, for example Hindi. Case does not matter.")
                    @RequestParam(required = false)
                    String language) {
        return ListResponse.of(movieService.listMovies(language));
    }

    @GetMapping("/{movieId}")
    @Operation(summary = "Get one movie")
    public MovieResponse getMovie(@PathVariable @Positive long movieId) {
        return movieService.getMovie(movieId);
    }
}
