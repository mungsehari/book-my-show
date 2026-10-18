package com.bookmyshow.modules.waitlist;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** All request and response types for the waitlist module. */
public final class WaitlistTypes {

    private WaitlistTypes() {}

    public record JoinWaitlistRequest(
            @NotNull @Min(1) @Max(10) Integer seats) {}

    /**
     * position is the place in the queue while WAITING (1 is next), else null. When OFFERED, lockId holds the seats
     * until offerExpiresAt: start checkout with it.
     */
    public record WaitlistResponse(
            Long entryId,
            Long showId,
            Integer seatsWanted,
            String status,
            Long position,
            String lockId,
            List<Long> offeredSeatIds,
            Instant offerExpiresAt) {}
}
