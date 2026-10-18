package com.bookmyshow.modules.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One member's vote in a poll. Voting again changes this row. */
@Entity
@Table(name = "poll_votes")
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    protected PollVote() {}

    public PollVote(Long pollId, Long userId, Long showId, Instant votedAt) {
        this.pollId = pollId;
        this.userId = userId;
        this.showId = showId;
        this.votedAt = votedAt;
    }

    void change(long showId, Instant now) {
        this.showId = showId;
        this.votedAt = now;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getShowId() {
        return showId;
    }
}
