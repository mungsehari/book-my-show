package com.bookmyshow.modules.stats;

import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.stats.StatsTypes.DailyResponse;
import com.bookmyshow.modules.stats.StatsTypes.OccupancyResponse;
import com.bookmyshow.modules.stats.StatsTypes.SummaryResponse;
import com.bookmyshow.modules.stats.StatsTypes.TopMoviesResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin stats")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
public class StatsController {

    private static final String DAYS_HELP = "How many days back, today included (1 to 90)";

    private final StatsService statsService;
    private final Clock clock;

    public StatsController(StatsService statsService, Clock clock) {
        this.statsService = statsService;
        this.clock = clock;
    }

    @GetMapping("/stats/summary")
    @Operation(summary = "Sales, tickets, cancellations and pending refunds for your cinemas")
    public SummaryResponse summary(
            @Parameter(description = DAYS_HELP) @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days,
            @AuthenticationPrincipal Jwt jwt) {
        return statsService.summary(CurrentUser.from(jwt), days);
    }

    @GetMapping("/stats/daily")
    @Operation(summary = "Sales for each day, oldest first. Days without sales show zero")
    public DailyResponse daily(
            @Parameter(description = DAYS_HELP) @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days,
            @AuthenticationPrincipal Jwt jwt) {
        return statsService.daily(CurrentUser.from(jwt), days);
    }

    @GetMapping("/stats/top-movies")
    @Operation(summary = "Movies with the most revenue")
    public TopMoviesResponse topMovies(
            @Parameter(description = DAYS_HELP) @RequestParam(defaultValue = "7") @Min(1) @Max(90) int days,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit,
            @AuthenticationPrincipal Jwt jwt) {
        return statsService.topMovies(CurrentUser.from(jwt), days, limit);
    }

    @GetMapping("/stats/occupancy")
    @Operation(summary = "How full each show is on a date (today by default)")
    public OccupancyResponse occupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal Jwt jwt) {
        return statsService.occupancy(CurrentUser.from(jwt), date == null ? LocalDate.now(clock) : date);
    }

    @GetMapping("/bookings")
    @Operation(summary = "All bookings in your cinemas, newest first")
    public PageResponse<BookingResponse> bookings(
            @Parameter(description = "For example CONFIRMED, CANCELLED, NEEDS_REFUND")
                    @RequestParam(required = false)
                    @Pattern(regexp = "^[A-Z_]{3,20}$")
                    String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return statsService.bookings(CurrentUser.from(jwt), status, page, size);
    }
}
