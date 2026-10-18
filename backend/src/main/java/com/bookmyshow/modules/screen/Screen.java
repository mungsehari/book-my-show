package com.bookmyshow.modules.screen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A screen (auditorium) in a cinema. Stores the cinema ID only, so modules stay independent. */
@Entity
@Table(name = "screens")
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(nullable = false, length = 50)
    private String name;

    protected Screen() {}

    public Screen(Long cinemaId, String name) {
        this.cinemaId = cinemaId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public String getName() {
        return name;
    }
}
