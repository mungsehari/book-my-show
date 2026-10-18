package com.bookmyshow.modules.friend;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.friend.FriendTypes.FriendRequestCreate;
import com.bookmyshow.modules.friend.FriendTypes.FriendRequestResponse;
import com.bookmyshow.modules.friend.FriendTypes.FriendResponse;
import com.bookmyshow.modules.friend.FriendTypes.FriendsActivityResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
@Tag(name = "Friends")
@SecurityRequirement(name = "bearer")
public class FriendController {

    private final FriendService friendService;
    private final FriendsActivityService activityService;

    public FriendController(FriendService friendService, FriendsActivityService activityService) {
        this.friendService = friendService;
        this.activityService = activityService;
    }

    @GetMapping
    @Operation(summary = "Your friends")
    public ListResponse<FriendResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return ListResponse.of(friendService.list(CurrentUser.from(jwt)));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a friend request by exact email")
    public FriendRequestResponse sendRequest(
            @Valid @RequestBody FriendRequestCreate request, @AuthenticationPrincipal Jwt jwt) {
        return friendService.sendRequest(request.email(), CurrentUser.from(jwt));
    }

    @GetMapping("/requests")
    @Operation(summary = "Friend requests sent to you")
    public ListResponse<FriendRequestResponse> incoming(@AuthenticationPrincipal Jwt jwt) {
        return ListResponse.of(friendService.incoming(CurrentUser.from(jwt)));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Operation(summary = "Accept a friend request")
    public FriendRequestResponse accept(@PathVariable @Positive long requestId, @AuthenticationPrincipal Jwt jwt) {
        return friendService.accept(requestId, CurrentUser.from(jwt));
    }

    @PostMapping("/requests/{requestId}/decline")
    @Operation(summary = "Decline a friend request")
    public FriendRequestResponse decline(@PathVariable @Positive long requestId, @AuthenticationPrincipal Jwt jwt) {
        return friendService.decline(requestId, CurrentUser.from(jwt));
    }

    @PostMapping("/{friendUserId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a friend")
    public void remove(@PathVariable @Positive long friendUserId, @AuthenticationPrincipal Jwt jwt) {
        friendService.remove(friendUserId, CurrentUser.from(jwt));
    }

    @GetMapping("/activity")
    @Operation(summary = "Which friends watched a movie, and which have tickets for it")
    public FriendsActivityResponse activity(@RequestParam @Positive long movieId, @AuthenticationPrincipal Jwt jwt) {
        return activityService.forMovie(movieId, CurrentUser.from(jwt));
    }
}
