package com.bookmyshow.modules.screen;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenViewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Screens")
public class ScreenController {

    private final ScreenService screenService;
    private final ScreenViewService screenViewService;

    public ScreenController(ScreenService screenService, ScreenViewService screenViewService) {
        this.screenService = screenService;
        this.screenViewService = screenViewService;
    }

    @GetMapping("/api/v1/screens/{screenId}/views")
    @Operation(summary = "Seat view photos: what the screen looks like from each block of rows")
    public ListResponse<ScreenViewResponse> listViews(@PathVariable @Positive long screenId) {
        return ListResponse.of(screenViewService.list(screenId));
    }

    @GetMapping("/api/v1/cinemas/{cinemaId}/screens")
    @Operation(summary = "List screens in a cinema")
    public ListResponse<ScreenResponse> listByCinema(@PathVariable @Positive long cinemaId) {
        return ListResponse.of(screenService.listByCinema(cinemaId));
    }

    @GetMapping("/api/v1/screens/{screenId}/layout")
    @Operation(summary = "Get the seat layout of a screen, grouped by row")
    public LayoutResponse getLayout(@PathVariable @Positive long screenId) {
        return screenService.getLayout(screenId);
    }
}
