package com.bookmyshow.modules.cinema;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.cinema.CinemaTypes.CreatePlaceRequest;
import com.bookmyshow.modules.cinema.CinemaTypes.PlaceResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Cinema places")
public class CinemaPlaceController {

    private final CinemaPlaceService placeService;

    public CinemaPlaceController(CinemaPlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/api/v1/cinemas/{cinemaId}/places")
    @Operation(summary = "Food and parking near a cinema, nearest first")
    public ListResponse<PlaceResponse> list(@PathVariable @Positive long cinemaId) {
        return ListResponse.of(placeService.list(cinemaId));
    }

    @PostMapping("/api/v1/admin/cinemas/{cinemaId}/places")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Add a food or parking place near a cinema")
    public PlaceResponse add(
            @PathVariable @Positive long cinemaId,
            @Valid @RequestBody CreatePlaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return placeService.add(cinemaId, request, CurrentUser.from(jwt));
    }
}
