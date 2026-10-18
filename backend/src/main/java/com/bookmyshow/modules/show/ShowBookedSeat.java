package com.bookmyshow.modules.show;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A seat sold for a show. Only one active row (releasedAt null) per show and seat. Stores the booking ID only, so
 * modules stay independent.
 */
@Entity
@Table(name = "show_booked_seats")
public class ShowBookedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Set when the sale is undone (cancel). The row stays as history, and the seat can be sold again. */
    @Column(name = "released_at")
    private Instant releasedAt;

    protected ShowBookedSeat() {}

    void release(Instant now) {
        this.releasedAt = now;
    }

    /** Resale: the same sold seat now belongs to the buyer's booking. */
    void transferTo(long newBookingId) {
        this.bookingId = newBookingId;
    }

    public ShowBookedSeat(Long showId, Long seatId, Long bookingId, Instant createdAt) {
        this.showId = showId;
        this.seatId = seatId;
        this.bookingId = bookingId;
        this.createdAt = createdAt;
    }

    public Long getSeatId() {
        return seatId;
    }

    public Long getShowId() {
        return showId;
    }
}
