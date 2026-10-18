package com.bookmyshow.modules.show;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** All request and response types for the show module. */
public final class ShowTypes {

    private ShowTypes() {}

    public record ShowResponse(
            Long id, Long cinemaId, String movieName, LocalDate showDate, LocalTime startTime, BigDecimal price) {

        static ShowResponse from(Show show) {
            return new ShowResponse(
                    show.getId(),
                    show.getCinemaId(),
                    show.getMovieName(),
                    show.getShowDate(),
                    show.getStartTime(),
                    show.getPrice());
        }
    }
}
