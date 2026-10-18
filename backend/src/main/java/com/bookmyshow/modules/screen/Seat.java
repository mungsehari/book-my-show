package com.bookmyshow.modules.screen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A fixed seat on a screen. Row is a single letter A to Z. Category is REGULAR, PREMIUM, or RECLINER. */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "screen_id", nullable = false)
    private Long screenId;

    @Column(name = "row_label", nullable = false, length = 1)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(nullable = false, length = 20)
    private String category;

    protected Seat() {}

    public Seat(Long screenId, String rowLabel, Integer seatNumber, String category) {
        this.screenId = screenId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Long getScreenId() {
        return screenId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public String getCategory() {
        return category;
    }
}
