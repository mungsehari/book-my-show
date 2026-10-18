package com.bookmyshow.modules.poll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** All request and response types for the poll module. */
public final class PollTypes {

    private PollTypes() {}

    /** 2 to 5 upcoming shows, and up to 20 of your friends. */
    public record CreatePollRequest(
            @NotBlank @Size(max = 100) String title,
            @NotNull @Size(min = 2, max = 5) List<@NotNull @Positive Long> showIds,
            @NotNull @Size(max = 20) List<@NotNull @Positive Long> friendIds) {}

    public record VoteRequest(@NotNull @Positive Long showId) {}

    public record PollOptionResponse(
            Long showId, String movieName, Long cinemaId, LocalDate showDate, LocalTime startTime, long votes) {}

    public record PollMemberResponse(Long userId, String name, boolean voted) {}

    /** myVote is the show you voted for, or null. winningShowId is set when the poll is CLOSED. */
    public record PollResponse(
            Long id,
            String title,
            String status,
            Long creatorId,
            List<PollOptionResponse> options,
            List<PollMemberResponse> members,
            Long myVote,
            Long winningShowId) {}
}
