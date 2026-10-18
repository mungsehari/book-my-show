package com.bookmyshow.modules.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A friend in a split booking. Members can see the booking and its ticket. */
@Entity
@Table(name = "booking_members")
public class BookingMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    protected BookingMember() {}

    public BookingMember(Long bookingId, Long userId) {
        this.bookingId = bookingId;
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
