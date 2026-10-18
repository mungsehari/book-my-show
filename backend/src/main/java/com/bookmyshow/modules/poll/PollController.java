package com.bookmyshow.modules.poll;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.poll.PollTypes.CreatePollRequest;
import com.bookmyshow.modules.poll.PollTypes.PollResponse;
import com.bookmyshow.modules.poll.PollTypes.VoteRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/polls")
@Tag(name = "Group polls")
@SecurityRequirement(name = "bearer")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ask friends to vote on 2 to 5 upcoming shows")
    public PollResponse create(@Valid @RequestBody CreatePollRequest request, @AuthenticationPrincipal Jwt jwt) {
        return pollService.create(request, CurrentUser.from(jwt));
    }

    @GetMapping("/mine")
    @Operation(summary = "Polls you are in, newest first")
    public ListResponse<PollResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return ListResponse.of(pollService.mine(CurrentUser.from(jwt)));
    }

    @GetMapping("/{pollId}")
    @Operation(summary = "A poll you are in, with vote counts")
    public PollResponse get(@PathVariable @Positive long pollId, @AuthenticationPrincipal Jwt jwt) {
        return pollService.get(pollId, CurrentUser.from(jwt));
    }

    @PostMapping("/{pollId}/vote")
    @Operation(summary = "Vote for a show. Voting again changes your vote.")
    public PollResponse vote(
            @PathVariable @Positive long pollId,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return pollService.vote(pollId, request.showId(), CurrentUser.from(jwt));
    }

    @PostMapping("/{pollId}/close")
    @Operation(summary = "Close the poll (creator only). The show with most votes wins.")
    public PollResponse close(@PathVariable @Positive long pollId, @AuthenticationPrincipal Jwt jwt) {
        return pollService.close(pollId, CurrentUser.from(jwt));
    }
}
