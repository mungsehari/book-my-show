package com.bookmyshow.modules.friend;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** All request and response types for the friend module. */
public final class FriendTypes {

    private FriendTypes() {}

    /** Friends are found by exact email only, so nobody can browse the user list. */
    public record FriendRequestCreate(
            @NotBlank @Email @Size(max = 255) String email) {}

    /** status is PENDING, or ACCEPTED when the other user had already asked you. */
    public record FriendRequestResponse(Long id, Long fromUserId, String fromName, String status, Instant createdAt) {}

    public record FriendResponse(Long userId, String name, Instant since) {}

    public record FriendSummary(Long userId, String name) {}

    /** Friends who share their activity: who watched this movie, and who has a ticket for an upcoming show. */
    public record FriendsActivityResponse(
            Long movieId, int watchedCount, List<FriendSummary> watched, int goingCount, List<FriendSummary> going) {}
}
