package com.bookmyshow.modules.movie;

import com.bookmyshow.modules.movie.MovieTypes.CreateMovieRequest;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Movies")
@SecurityRequirement(name = "bearer")
public class MovieAdminController {

    private final MovieService movieService;

    public MovieAdminController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping("/api/v1/admin/movies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a movie to the catalog")
    public MovieResponse create(@Valid @RequestBody CreateMovieRequest request) {
        return movieService.create(request);
    }
}
