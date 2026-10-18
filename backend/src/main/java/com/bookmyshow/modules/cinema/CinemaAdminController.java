package com.bookmyshow.modules.cinema;

import com.bookmyshow.modules.cinema.CinemaTypes.AssignAdminRequest;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Cinemas")
@SecurityRequirement(name = "bearer")
public class CinemaAdminController {

    private final CinemaAccessService cinemaAccessService;

    public CinemaAdminController(CinemaAccessService cinemaAccessService) {
        this.cinemaAccessService = cinemaAccessService;
    }

    @PostMapping("/api/v1/admin/cinemas/{cinemaId}/admins")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Let a user manage a cinema (makes them a theatre admin)")
    public CinemaAdminResponse assignAdmin(
            @PathVariable @Positive long cinemaId, @Valid @RequestBody AssignAdminRequest request) {
        return cinemaAccessService.assignAdmin(cinemaId, request.userId());
    }
}
