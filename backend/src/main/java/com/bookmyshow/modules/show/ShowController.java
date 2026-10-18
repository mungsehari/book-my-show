package com.bookmyshow.modules.show;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.movie.MovieTypes.MovieResponse;
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

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping("/api/v1/cinemas/{cinemaId}/shows")
    @Operation(summary = "List shows at a cinema on a date")
    public ListResponse<ShowResponse> listByCinema(
            @PathVariable @Positive long cinemaId,
            @Parameter(description = "Show date (yyyy-MM-dd). Defaults to today.")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return ListResponse.of(showService.listByCinema(cinemaId, date));
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
