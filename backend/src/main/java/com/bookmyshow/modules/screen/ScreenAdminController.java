package com.bookmyshow.modules.screen;

import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenRequest;
import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenViewRequest;
import com.bookmyshow.modules.screen.ScreenTypes.LayoutResponse;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenViewResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Screens")
@SecurityRequirement(name = "bearer")
public class ScreenAdminController {

    private final ScreenService screenService;
    private final ScreenViewService screenViewService;

    public ScreenAdminController(ScreenService screenService, ScreenViewService screenViewService) {
        this.screenService = screenService;
        this.screenViewService = screenViewService;
    }

    @PostMapping("/api/v1/admin/screens/{screenId}/views")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
    @Operation(summary = "Add a seat view photo (https URL) for a block of rows")
    public ScreenViewResponse addView(
            @PathVariable @Positive long screenId,
            @Valid @RequestBody CreateScreenViewRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return screenViewService.add(screenId, request, CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/admin/cinemas/{cinemaId}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
    @Operation(summary = "Add a screen with its seat layout to a cinema")
    public LayoutResponse create(
            @PathVariable @Positive long cinemaId,
            @Valid @RequestBody CreateScreenRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return screenService.create(cinemaId, request, CurrentUser.from(jwt));
    }
}
