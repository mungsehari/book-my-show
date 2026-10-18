package com.bookmyshow.modules.booking;

import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

    // Ticket details, copied from the show at checkout.

    @Column(name = "show_date", nullable = false)
    private LocalDate showDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "movie_name", nullable = false, length = 200)
    private String movieName;

    /** Null for old shows not linked to the catalog. */
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(name = "screen_name", nullable = false, length = 50)
    private String screenName;

    /** Comma-separated, like "A1,A2". */
    @Column(name = "seat_labels", nullable = false, length = 250)
    private String seatLabels;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(name = "razorpay_refund_id", length = 50)
    private String razorpayRefundId;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    /** Set while the owner offers the ticket for resale at face value. */
    @Column(name = "resale_listed_at")
    private Instant resaleListedAt;

    /** For a resale purchase: the seller's booking. */
    @Column(name = "resale_of_booking_id")
    private Long resaleOfBookingId;

    /** Set when the ticket was given to the current owner as a gift. */
    @Column(name = "gifted_from_user_id")
    private Long giftedFromUserId;

    protected Booking() {}

    /**
     * A buyer's booking for a resale ticket, waiting for payment. Same show, seats, and price as the seller's booking,
     * so the price can never go above face value.
     */
    static Booking resaleOf(Booking seller, long buyerId, String holdId, Instant createdAt) {
        Booking booking = new Booking();
        booking.userId = buyerId;
        booking.showId = seller.showId;
        booking.seatIds = seller.seatIds;
        booking.amount = seller.amount;
        booking.currency = seller.currency;
        booking.lockId = holdId;
        booking.createdAt = createdAt;
        booking.status = BookingStatus.PENDING_PAYMENT;
        booking.showDate = seller.showDate;
        booking.startTime = seller.startTime;
        booking.movieName = seller.movieName;
        booking.movieId = seller.movieId;
        booking.cinemaId = seller.cinemaId;
        booking.screenName = seller.screenName;
        booking.seatLabels = seller.seatLabels;
        booking.resaleOfBookingId = seller.id;
        return booking;
    }

    /** A new booking for the seats in a lock, waiting for payment. */
    public Booking(ActiveLock lock, String currency, Instant createdAt) {
        this.userId = lock.userId();
        this.showId = lock.showId();
        this.seatIds = lock.seatIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        this.amount = lock.totalPrice();
        this.currency = currency;
        this.lockId = lock.lockId();
        this.createdAt = createdAt;
        this.status = BookingStatus.PENDING_PAYMENT;
        this.showDate = lock.showDate();
        this.startTime = lock.startTime();
        this.movieName = lock.movieName();
        this.movieId = lock.movieId();
        this.cinemaId = lock.cinemaId();
        this.screenName = lock.screenName();
        this.seatLabels = String.join(",", lock.seatLabels());
    }

    /** Records the entry scan. */
    void checkIn(long staffUserId, Instant now) {
        this.checkedInBy = staffUserId;
        this.checkedInAt = now;
    }

    void attachOrder(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    /** Payment can still be applied: waiting, failed earlier and paid on a retry, or expired and paid late. */
    boolean awaitsPayment() {
        return BookingStatus.PENDING_PAYMENT.equals(status)
                || BookingStatus.FAILED.equals(status)
                || BookingStatus.EXPIRED.equals(status);
    }

    /** Not paid in time. A late payment can still confirm it if the seats are free. */
    void markExpired() {
        this.status = BookingStatus.EXPIRED;
    }

    /** The user cancelled and got the money back. */
    void cancel(String refundId, Instant now) {
        this.status = BookingStatus.CANCELLED;
        this.razorpayRefundId = refundId;
        this.cancelledAt = now;
        this.refundedAt = now;
        this.resaleListedAt = null;
    }

    /** A NEEDS_REFUND booking got its money back. */
    void markRefunded(String refundId, Instant now) {
        this.status = BookingStatus.REFUNDED;
        this.razorpayRefundId = refundId;
        this.refundedAt = now;
    }

    void listForResale(Instant now) {
        this.resaleListedAt = now;
    }

    void unlistResale() {
        this.resaleListedAt = null;
    }

    /** The ticket was bought by someone else at face value, and this owner got the money back. */
    void markResold(String refundId, Instant now) {
        this.status = BookingStatus.RESOLD;
        this.razorpayRefundId = refundId;
        this.refundedAt = now;
        this.resaleListedAt = null;
    }

    /** The ticket now belongs to someone else. No money moves. */
    void giftTo(long recipientUserId) {
        this.giftedFromUserId = this.userId;
        this.userId = recipientUserId;
    }

    public Long getGiftedFromUserId() {
        return giftedFromUserId;
    }

    boolean isListedForResale() {
        return resaleListedAt != null;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public Long getResaleOfBookingId() {
        return resaleOfBookingId;
    }

    public Instant getResaleListedAt() {
        return resaleListedAt;
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

    public LocalDate getShowDate() {
        return showDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getMovieName() {
        return movieName;
    }

    public Long getMovieId() {
        return movieId;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public String getScreenName() {
        return screenName;
    }

    public List<String> getSeatLabels() {
        return List.of(seatLabels.split(","));
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }
}
