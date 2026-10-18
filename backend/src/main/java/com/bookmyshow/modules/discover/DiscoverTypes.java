package com.bookmyshow.modules.discover;

import com.bookmyshow.integrations.ai.MovieFilters;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** All request and response types for the discover module. */
public final class DiscoverTypes {

    private DiscoverTypes() {}

    /** source is "claude", "cache", or "keyword": how the request was read. */
    public record MoodSearchResponse(String query, MovieFilters filters, String source, List<MovieResponse> movies) {}

    /** mood is optional free text. time is MORNING (before 12), AFTERNOON (12 to 5 PM), or EVENING (from 5 PM). */
    public record PlanRequest(
            @NotNull @Positive Long cityId,
            @NotNull LocalDate date,
            @Size(max = 200) String mood,
            @NotNull @Min(1) @Max(10) Integer people,
            @DecimalMin("1.00") BigDecimal budgetPerPerson,

            @NotNull @Pattern(regexp = "^(MORNING|AFTERNOON|EVENING)$")
            String time) {}

    public record PlaceSummary(String name, Integer walkMinutes, String notes) {}

    /** pricePerPerson is the cheapest free seat right now (deals included); totalPrice is for the whole group. */
    public record PlanOption(
            Long showId,
            Long movieId,
            String movieName,
            Long cinemaId,
            String cinemaName,
            LocalTime startTime,
            int seatsAvailable,
            BigDecimal pricePerPerson,
            BigDecimal totalPrice,
            PlaceSummary food,
            PlaceSummary parking) {}

    public record PlanResponse(LocalDate date, MovieFilters filters, String source, List<PlanOption> plans) {}
}
