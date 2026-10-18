package com.bookmyshow.modules.show;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** All request and response types for the show module. */
public final class ShowTypes {

    private ShowTypes() {}

    /**
     * Prices by seat category, for example {"REGULAR": 200, "PREMIUM": 300}. Every category on the screen needs one.
     * quiet, sensoryFriendly, and subtitles are optional and default to false.
     */
    public record CreateShowRequest(
            @NotNull @Positive Long movieId,
            @NotNull LocalDate showDate,
            @NotNull LocalTime startTime,

            @NotEmpty
            Map<
                            @NotNull @Pattern(regexp = "^(REGULAR|PREMIUM|RECLINER)$") String,
                            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal>
                    prices,

            Boolean quiet,
            Boolean sensoryFriendly,
            Boolean subtitles) {}

    /** Optional filters for a cinema's shows. A null or false filter is not applied. */
    public record ShowFilters(boolean quiet, boolean sensoryFriendly, boolean subtitles, boolean wheelchair) {

        public static final ShowFilters NONE = new ShowFilters(false, false, false, false);
    }

    /** price is the lowest category price, shown as "from" in listings. */
    public record ShowResponse(
            Long id,
            Long cinemaId,
            Long screenId,
            Long movieId,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            BigDecimal price,
            boolean quiet,
            boolean sensoryFriendly,
            boolean subtitles) {

        static ShowResponse from(Show show) {
            return new ShowResponse(
                    show.getId(),
                    show.getCinemaId(),
                    show.getScreenId(),
                    show.getMovieId(),
                    show.getMovieName(),
                    show.getShowDate(),
                    show.getStartTime(),
                    show.getPrice(),
                    show.isQuiet(),
                    show.isSensoryFriendly(),
                    show.hasSubtitles());
        }
    }

    public record CategoryPrice(String category, BigDecimal price) {}

    /**
     * How full a show is right now. Counts only seats for sale. percentFull counts sold and held seats. level is LOW
     * (under 40%), MEDIUM (under 75%), HIGH (under 100%), or HOUSEFULL.
     */
    public record CrowdResponse(
            Long showId, int totalSeats, int sold, int held, int available, int percentFull, String level) {}

    /** status is one of {@link SeatStatus}. price is null when the show has no price for the seat's category. */
    public record SeatMapSeat(
            Long id, Integer number, String category, BigDecimal price, String status, boolean wheelchair) {}

    public record SeatMapRow(String label, List<SeatMapSeat> seats) {}

    public record SeatMapResponse(
            Long showId,
            Long movieId,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            Long screenId,
            String screenName,
            List<CategoryPrice> prices,
            List<SeatMapRow> rows) {}

    public record LockSeatsRequest(@NotEmpty @Size(max = 10) List<@NotNull @Positive Long> seatIds) {}

    /** The seats stay held for this user until expiresAt. totalPrice is the sum of the seat prices. */
    public record SeatLockResponse(
            String lockId, Long showId, List<Long> seatIds, BigDecimal totalPrice, Instant expiresAt) {}

    /**
     * A lock that is still held, with what it costs now and the show details a ticket needs (copied onto the booking). Used by the booking
     * module. seatLabels are like "A1", in the same order as seatIds.
     */
    public record ActiveLock(
            String lockId,
            long userId,
            long showId,
            List<Long> seatIds,
            BigDecimal totalPrice,
            long cinemaId,
            String screenName,
            String movieName,
            LocalDate showDate,
            LocalTime startTime,
            List<String> seatLabels,
            Long movieId) {}

    /** Seat states on a show's seat map. */
    public static final class SeatStatus {

        public static final String AVAILABLE = "AVAILABLE";
        /** Sold. Never frees itself. */
        public static final String BOOKED = "BOOKED";
        /** Held by someone during checkout. Frees itself when the lock expires. */
        public static final String LOCKED = "LOCKED";
        /** The show has no price for this seat's category, so it cannot be sold. */
        public static final String UNAVAILABLE = "UNAVAILABLE";

        private SeatStatus() {}
    }
}
