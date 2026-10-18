package com.bookmyshow.modules.show;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** All request and response types for the show module. */
public final class ShowTypes {

    private ShowTypes() {}

    public record CreateShowRequest(
            @NotNull @Positive Long movieId,
            @NotNull LocalDate showDate,
            @NotNull LocalTime startTime,

            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2)
            BigDecimal price) {}

    public record ShowResponse(
            Long id,
            Long cinemaId,
            Long screenId,
            Long movieId,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            BigDecimal price) {

        static ShowResponse from(Show show) {
            return new ShowResponse(
                    show.getId(),
                    show.getCinemaId(),
                    show.getScreenId(),
                    show.getMovieId(),
                    show.getMovieName(),
                    show.getShowDate(),
                    show.getStartTime(),
                    show.getPrice());
        }
    }
}
