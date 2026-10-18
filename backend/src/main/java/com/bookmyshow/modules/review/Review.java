package com.bookmyshow.modules.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A movie review by someone who watched it. bookingId is the proof. Stores IDs only, so modules stay independent. */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Review() {}

    public Review(Long movieId, Long userId, Long bookingId, Integer rating, String text, Instant createdAt) {
        this.movieId = movieId;
        this.userId = userId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMovieId() {
        return movieId;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
