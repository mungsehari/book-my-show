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

    private BookingStatus() {}
}
