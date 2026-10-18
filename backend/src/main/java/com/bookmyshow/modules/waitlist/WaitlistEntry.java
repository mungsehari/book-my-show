package com.bookmyshow.modules.waitlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A user waiting for seats in a full show. WAITING, then OFFERED (seats held for them for a few minutes), then BOOKED
 * or EXPIRED. LEFT if they leave the queue. Stores IDs only, so modules stay independent.
 */
@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seats_wanted", nullable = false)
    private Integer seatsWanted;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "lock_id", length = 36)
    private String lockId;

    /** Comma-separated seat IDs held for the user. */
    @Column(name = "offered_seat_ids", length = 250)
    private String offeredSeatIds;

    @Column(name = "offered_at")
    private Instant offeredAt;

    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WaitlistEntry() {}

    public WaitlistEntry(Long showId, Long userId, Integer seatsWanted, Instant createdAt) {
        this.showId = showId;
        this.userId = userId;
        this.seatsWanted = seatsWanted;
        this.createdAt = createdAt;
        this.status = WaitlistStatus.WAITING;
    }

    void offer(String lockId, List<Long> seatIds, Instant now, Instant expiresAt) {
        this.status = WaitlistStatus.OFFERED;
        this.lockId = lockId;
        this.offeredSeatIds = seatIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        this.offeredAt = now;
        this.offerExpiresAt = expiresAt;
    }

    void markBooked() {
        this.status = WaitlistStatus.BOOKED;
    }

    void markExpired() {
        this.status = WaitlistStatus.EXPIRED;
    }

    void leave() {
        this.status = WaitlistStatus.LEFT;
    }

    boolean isActive() {
        return WaitlistStatus.WAITING.equals(status) || WaitlistStatus.OFFERED.equals(status);
    }

    public Long getId() {
        return id;
    }

    public Long getShowId() {
        return showId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getSeatsWanted() {
        return seatsWanted;
    }

    public String getStatus() {
        return status;
    }

    public String getLockId() {
        return lockId;
    }

    public List<Long> getOfferedSeatIds() {
        return offeredSeatIds == null || offeredSeatIds.isEmpty()
                ? List.of()
                : Arrays.stream(offeredSeatIds.split(",")).map(Long::valueOf).toList();
    }

    public Instant getOfferExpiresAt() {
        return offerExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
