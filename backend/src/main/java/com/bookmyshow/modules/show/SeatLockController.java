package com.bookmyshow.modules.show;

import com.bookmyshow.modules.show.ShowTypes.LockSeatsRequest;
import com.bookmyshow.modules.show.ShowTypes.SeatLockResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Seat locks")
@SecurityRequirement(name = "bearer")
public class SeatLockController {

    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    private final ShowSeatLockService seatLockService;

    public SeatLockController(ShowSeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }

    @PostMapping("/api/v1/shows/{showId}/locks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Hold up to 10 seats for 10 minutes while you pay. All seats are held, or none.")
    public SeatLockResponse lock(
            @PathVariable @Positive long showId,
            @Valid @RequestBody LockSeatsRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return seatLockService.lock(showId, request.seatIds(), CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/locks/{lockId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Give back held seats before the lock expires")
    public void release(@PathVariable @Pattern(regexp = UUID_PATTERN) String lockId, @AuthenticationPrincipal Jwt jwt) {
        seatLockService.release(lockId, CurrentUser.from(jwt));
    }
}
