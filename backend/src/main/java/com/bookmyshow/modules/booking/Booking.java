package com.bookmyshow.modules.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** A ticket purchase. See {@link BookingStatus} for the states. Stores IDs only, so modules stay independent. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false, length = 20)
    private String status;

    /** Comma-separated seat IDs, at most 10. */
    @Column(name = "seat_ids", nullable = false, length = 250)
    private String seatIds;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "lock_id", nullable = false, length = 36)
    private String lockId;

    @Column(name = "razorpay_order_id", length = 50)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected Booking() {}

    public Booking(
            Long userId,
            Long showId,
            List<Long> seatIds,
            BigDecimal amount,
            String currency,
            String lockId,
            Instant createdAt) {
        this.userId = userId;
        this.showId = showId;
        this.seatIds = seatIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        this.amount = amount;
        this.currency = currency;
        this.lockId = lockId;
        this.createdAt = createdAt;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    void attachOrder(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    /** Payment can still be applied: waiting, or failed earlier and paid on a retry. */
    boolean awaitsPayment() {
        return BookingStatus.PENDING_PAYMENT.equals(status) || BookingStatus.FAILED.equals(status);
    }

    void confirm(String paymentId, Instant now) {
        this.razorpayPaymentId = paymentId;
        this.status = BookingStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    /** Paid, but the seats were sold to someone else first. Money must go back. */
    void markNeedsRefund(String paymentId) {
        this.razorpayPaymentId = paymentId;
        this.status = BookingStatus.NEEDS_REFUND;
    }

    void markFailed() {
        this.status = BookingStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getShowId() {
        return showId;
    }

    public String getStatus() {
        return status;
    }

    public List<Long> getSeatIds() {
        return Arrays.stream(seatIds.split(",")).map(Long::valueOf).toList();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getLockId() {
        return lockId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
