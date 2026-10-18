package com.bookmyshow.modules.cinema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Gives a theatre admin the right to manage one cinema. Stores IDs only, so modules stay independent. */
@Entity
@Table(name = "cinema_admins")
public class CinemaAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CinemaAdmin() {}

    public CinemaAdmin(Long userId, Long cinemaId, Instant createdAt) {
        this.userId = userId;
        this.cinemaId = cinemaId;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCinemaId() {
        return cinemaId;
    }
}
