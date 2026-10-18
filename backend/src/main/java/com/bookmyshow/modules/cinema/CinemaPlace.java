package com.bookmyshow.modules.cinema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A place near a cinema: somewhere to eat (FOOD) or to park (PARKING). Entered by the theatre. */
@Entity
@Table(name = "cinema_places")
public class CinemaPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "walk_minutes", nullable = false)
    private Integer walkMinutes;

    @Column(length = 300)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CinemaPlace() {}

    public CinemaPlace(Long cinemaId, String type, String name, Integer walkMinutes, String notes, Instant createdAt) {
        this.cinemaId = cinemaId;
        this.type = type;
        this.name = name;
        this.walkMinutes = walkMinutes;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Integer getWalkMinutes() {
        return walkMinutes;
    }

    public String getNotes() {
        return notes;
    }
}
