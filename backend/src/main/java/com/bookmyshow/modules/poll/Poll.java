package com.bookmyshow.modules.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A group poll: friends vote on which show to watch. OPEN until the creator closes it. */
@Entity
@Table(name = "polls")
public class Poll {

    static final String OPEN = "OPEN";
    static final String CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "winning_show_id")
    private Long winningShowId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected Poll() {}

    public Poll(Long creatorId, String title, Instant createdAt) {
        this.creatorId = creatorId;
        this.title = title;
        this.createdAt = createdAt;
        this.status = OPEN;
    }

    void close(Long winningShowId, Instant now) {
        this.status = CLOSED;
        this.winningShowId = winningShowId;
        this.closedAt = now;
    }

    boolean isOpen() {
        return OPEN.equals(status);
    }

    public Long getId() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public Long getWinningShowId() {
        return winningShowId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
