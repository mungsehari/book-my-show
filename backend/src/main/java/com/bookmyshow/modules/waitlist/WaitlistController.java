package com.bookmyshow.modules.waitlist;

import com.bookmyshow.modules.waitlist.WaitlistTypes.JoinWaitlistRequest;
import com.bookmyshow.modules.waitlist.WaitlistTypes.WaitlistResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Waitlist")
@SecurityRequirement(name = "bearer")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/api/v1/shows/{showId}/waitlist")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Join the waitlist of a full show")
    public WaitlistResponse join(
            @PathVariable @Positive long showId,
            @Valid @RequestBody JoinWaitlistRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return waitlistService.join(showId, request.seats(), CurrentUser.from(jwt));
    }

    @GetMapping("/api/v1/waitlist/me")
    @Operation(summary = "Your place in a show's waitlist, or the seats held for you")
    public WaitlistResponse myEntry(@RequestParam @Positive long showId, @AuthenticationPrincipal Jwt jwt) {
        return waitlistService.myEntry(showId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/waitlist/{entryId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Leave the waitlist")
    public void leave(@PathVariable @Positive long entryId, @AuthenticationPrincipal Jwt jwt) {
        waitlistService.leave(entryId, CurrentUser.from(jwt));
    }
}
