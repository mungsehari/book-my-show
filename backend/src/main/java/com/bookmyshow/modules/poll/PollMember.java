package com.bookmyshow.modules.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A user who may see and vote in a poll. The creator is a member too. */
@Entity
@Table(name = "poll_members")
public class PollMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected PollMember() {}

    public PollMember(Long pollId, Long userId) {
        this.pollId = pollId;
        this.userId = userId;
    }

    public Long getPollId() {
        return pollId;
    }

    public Long getUserId() {
        return userId;
    }
}
