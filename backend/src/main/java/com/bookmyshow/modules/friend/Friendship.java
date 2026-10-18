package com.bookmyshow.modules.friend;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The friendship state between two users. One row per pair. PENDING until the addressee answers. A DECLINED or REMOVED
 * pair is reused when someone asks again. Stores IDs only, so modules stay independent.
 */
@Entity
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Friendship() {}

    public Friendship(Long requesterId, Long addresseeId, Instant createdAt) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.createdAt = createdAt;
        this.status = FriendshipStatus.PENDING;
    }

    /** A new request on an old (declined or removed) pair. */
    void reopen(long requesterId, long addresseeId, Instant now) {
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.createdAt = now;
        this.respondedAt = null;
        this.status = FriendshipStatus.PENDING;
    }

    void accept(Instant now) {
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = now;
    }

    void decline(Instant now) {
        this.status = FriendshipStatus.DECLINED;
        this.respondedAt = now;
    }

    void remove(Instant now) {
        this.status = FriendshipStatus.REMOVED;
        this.respondedAt = now;
    }

    /** The other user in the pair. */
    long otherThan(long userId) {
        return requesterId == userId ? addresseeId : requesterId;
    }

    public Long getId() {
        return id;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public Long getAddresseeId() {
        return addresseeId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
