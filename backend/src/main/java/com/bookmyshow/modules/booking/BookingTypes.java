package com.bookmyshow.modules.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** All request and response types for the booking module. */
public final class BookingTypes {

    private BookingTypes() {}

    public record CreateBookingRequest(
            @NotBlank @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            String lockId) {}

    /** What the frontend needs to open Razorpay checkout. amountInPaise is what Razorpay charges. */
    public record CheckoutResponse(
            Long bookingId,
            String status,
            BigDecimal amount,
            long amountInPaise,
            String currency,
            String razorpayOrderId,
            String razorpayKeyId) {}

    /** The three values Razorpay checkout returns after a successful payment. */
    public record ConfirmPaymentRequest(
            @NotBlank @Size(max = 50) String razorpayOrderId,
            @NotBlank @Size(max = 50) String razorpayPaymentId,
            @NotBlank @Size(max = 128) String razorpaySignature) {}

    public record BookingResponse(
            Long id,
            Long showId,
            List<Long> seatIds,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt,
            Instant confirmedAt) {

        static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.getId(),
                    booking.getShowId(),
                    booking.getSeatIds(),
                    booking.getStatus(),
                    booking.getAmount(),
                    booking.getCurrency(),
                    booking.getCreatedAt(),
                    booking.getConfirmedAt());
        }
    }
}
