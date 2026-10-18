package com.bookmyshow.modules.show;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A seat sold for a show. Unique per show and seat. Stores the booking ID only, so modules stay independent. */
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

    protected ShowBookedSeat() {}

    public ShowBookedSeat(Long showId, Long seatId, Long bookingId, Instant createdAt) {
        this.showId = showId;
        this.seatId = seatId;
        this.bookingId = bookingId;
        this.createdAt = createdAt;
    }

    public Long getSeatId() {
        return seatId;
    }
}
