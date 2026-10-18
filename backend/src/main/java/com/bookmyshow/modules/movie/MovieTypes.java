package com.bookmyshow.modules.movie;

import java.time.LocalDate;

/** All request and response types for the movie module. */
public final class MovieTypes {

    private MovieTypes() {}

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
