package com.bookmyshow.modules.show;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.show.ShowTypes.SeatMapResponse;
import com.bookmyshow.modules.show.ShowTypes.ShowFilters;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Shows")
public class ShowController {

    private final ShowService showService;
    private final ShowSeatMapService showSeatMapService;
    private final ShowCrowdService showCrowdService;

    public ShowController(
            ShowService showService, ShowSeatMapService showSeatMapService, ShowCrowdService showCrowdService) {
        this.showService = showService;
        this.showSeatMapService = showSeatMapService;
        this.showCrowdService = showCrowdService;
    }

    @GetMapping("/api/v1/shows/{showId}/crowd")
    @Operation(summary = "Crowd meter: how full a show is right now")
    public CrowdResponse getCrowd(@PathVariable @Positive long showId) {
        return showCrowdService.getCrowd(showId);
    }

    @GetMapping("/api/v1/shows/{showId}/seats")
    @Operation(summary = "Seat map of a show, with the price and status of every seat")
    public SeatMapResponse getSeatMap(@PathVariable @Positive long showId) {
        return showSeatMapService.getSeatMap(showId);
    }

    @GetMapping("/api/v1/cinemas/{cinemaId}/shows")
    @Operation(summary = "List shows at a cinema on a date")
    public ListResponse<ShowResponse> listByCinema(
            @PathVariable @Positive long cinemaId,
            @Parameter(description = "Show date (yyyy-MM-dd). Defaults to today.")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @Parameter(description = "Only quiet shows") @RequestParam(defaultValue = "false") boolean quiet,
            @Parameter(description = "Only sensory-friendly shows") @RequestParam(defaultValue = "false")
                    boolean sensoryFriendly,
            @Parameter(description = "Only shows with subtitles") @RequestParam(defaultValue = "false")
                    boolean subtitles,
            @Parameter(description = "Only shows on screens with wheelchair seats")
                    @RequestParam(defaultValue = "false")
                    boolean wheelchair) {
        return ListResponse.of(showService.listByCinema(
                cinemaId, date, new ShowFilters(quiet, sensoryFriendly, subtitles, wheelchair)));
    }

    @GetMapping("/api/v1/cities/{cityId}/movies")
    @Operation(summary = "List movies playing in a city")
    public ListResponse<MovieResponse> listMoviesInCity(
            @PathVariable @Positive long cityId,
            @Parameter(description = "Show date (yyyy-MM-dd). Defaults to the next 7 days, today included.")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return ListResponse.of(showService.listMoviesInCity(cityId, date));
    }
}
