package com.bookmyshow.modules.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * One person's part of a split booking, with its own Razorpay order. PENDING, then PAID. REFUNDED if the booking could
 * not complete after they paid. VOID if the booking expired before they paid (no money moved).
 */
@Entity
@Table(name = "payment_shares")
public class PaymentShare {

    static final String PENDING = "PENDING";
    static final String PAID = "PAID";
    static final String REFUNDED = "REFUNDED";
    static final String VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "razorpay_order_id", length = 50)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "razorpay_refund_id", length = 50)
    private String razorpayRefundId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    protected PaymentShare() {}

    public PaymentShare(Long bookingId, Long userId, BigDecimal amount, String currency, Instant createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
        this.status = PENDING;
    }

    void attachOrder(String orderId) {
        this.razorpayOrderId = orderId;
    }

    void markPaid(String paymentId, Instant now) {
        this.status = PAID;
        this.razorpayPaymentId = paymentId;
        this.paidAt = now;
    }

    void markRefunded(String refundId, Instant now) {
        this.status = REFUNDED;
        this.razorpayRefundId = refundId;
        this.refundedAt = now;
    }

    void markVoid() {
        this.status = VOID;
    }

    boolean isPending() {
        return PENDING.equals(status);
    }

    boolean isPaid() {
        return PAID.equals(status);
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }
}
