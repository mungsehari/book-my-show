package com.bookmyshow.modules.poll;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A show people can vote for in a poll. */
@Entity
@Table(name = "poll_options")
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    protected PollOption() {}

    public PollOption(Long pollId, Long showId) {
        this.pollId = pollId;
        this.showId = showId;
    }

    public Long getShowId() {
        return showId;
    }
}
