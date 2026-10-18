package com.bookmyshow.modules.show;

import com.bookmyshow.modules.show.ShowTypes.CreateShowRequest;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
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
@Tag(name = "Admin: Shows")
@SecurityRequirement(name = "bearer")
public class ShowAdminController {

    private final ShowService showService;

    public ShowAdminController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping("/api/v1/admin/screens/{screenId}/shows")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
    @Operation(summary = "Add a show to a screen. Fails if it overlaps another show on that screen.")
    public ShowResponse create(
            @PathVariable @Positive long screenId,
            @Valid @RequestBody CreateShowRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return showService.create(screenId, request, CurrentUser.from(jwt));
    }
}
