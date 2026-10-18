package com.bookmyshow.modules.booking;

/** Booking states. Stored as text in bookings.status. */
public final class BookingStatus {

    /** Order created, waiting for the user to pay. */
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    /** Paid and seats sold. */
    public static final String CONFIRMED = "CONFIRMED";
    /** The payment failed. A retry on the same order can still confirm it. */
    public static final String FAILED = "FAILED";
    /** Paid, but a seat was sold to someone else first. The money must be refunded. */
    public static final String NEEDS_REFUND = "NEEDS_REFUND";
    /** Not paid in time. A late payment can still confirm it if the seats are free. */
    public static final String EXPIRED = "EXPIRED";
    /** Cancelled by the user and refunded. The seats were freed. */
    public static final String CANCELLED = "CANCELLED";
    /** A NEEDS_REFUND booking that got its money back. */
    public static final String REFUNDED = "REFUNDED";
    /** Sold on to another user at face value. This owner was refunded. */
    public static final String RESOLD = "RESOLD";

    private BookingStatus() {}
}
