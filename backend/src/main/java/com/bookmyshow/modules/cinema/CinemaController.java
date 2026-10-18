package com.bookmyshow.modules.cinema;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @GetMapping("/api/v1/cities/{cityId}/cinemas")
    @Operation(summary = "List cinemas in a city")
    public ListResponse<CinemaResponse> listByCity(@PathVariable @Positive long cityId) {
        return ListResponse.of(cinemaService.listByCity(cityId));
    }
}
