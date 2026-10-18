package com.bookmyshow.modules.movie;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** All request and response types for the movie module. */
public final class MovieTypes {

    private MovieTypes() {}

    public record CreateMovieRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 1000) String description,
            @NotBlank @Size(max = 50) String language,
            @NotBlank @Size(max = 50) String genre,
            @NotNull @Positive @Max(600) Integer durationMinutes,
            @NotNull @Pattern(regexp = "^(U|UA|A)$") String certificate,
            @NotNull LocalDate releaseDate,

            @Size(max = 500) @Pattern(regexp = "^https://.+")
            String posterUrl) {}

    public record MovieResponse(
            Long id,
            String title,
            String description,
            String language,
            String genre,
            Integer durationMinutes,
            String certificate,
            LocalDate releaseDate,
            String posterUrl) {

        static MovieResponse from(Movie movie) {
            return new MovieResponse(
                    movie.getId(),
                    movie.getTitle(),
                    movie.getDescription(),
                    movie.getLanguage(),
                    movie.getGenre(),
                    movie.getDurationMinutes(),
                    movie.getCertificate(),
                    movie.getReleaseDate(),
                    movie.getPosterUrl());
        }
    }
}
