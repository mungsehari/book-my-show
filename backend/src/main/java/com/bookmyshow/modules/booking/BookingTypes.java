package com.bookmyshow.modules.booking;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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
            String movieName,
            Long cinemaId,
            String screenName,
            LocalDate showDate,
            LocalTime startTime,
            List<Long> seatIds,
            List<String> seatLabels,
            String status,
            BigDecimal amount,
            String currency,
            Instant createdAt,
            Instant confirmedAt) {

        static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.getId(),
                    booking.getShowId(),
                    booking.getMovieName(),
                    booking.getCinemaId(),
                    booking.getScreenName(),
                    booking.getShowDate(),
                    booking.getStartTime(),
                    booking.getSeatIds(),
                    booking.getSeatLabels(),
                    booking.getStatus(),
                    booking.getAmount(),
                    booking.getCurrency(),
                    booking.getCreatedAt(),
                    booking.getConfirmedAt());
        }
    }

    /**
     * What the user shows at the entry. The frontend draws ticketCode as a QR code. The code is signed, so it cannot be
     * made up, and it works only once.
     */
    public record TicketResponse(
            Long bookingId,
            String ticketCode,
            String movieName,
            String cinemaName,
            String screenName,
            LocalDate showDate,
            LocalTime startTime,
            List<String> seats,
            BigDecimal amount,
            String currency,
            Instant checkedInAt) {}

    /** A ticket offered for resale. price is what the owner paid, the only price it can sell for. */
    public record ResaleListing(
            Long bookingId, Long showId, List<String> seats, BigDecimal price, String currency, boolean onHold) {}

    /** Used by friends activity: who watched a movie, and who has a ticket for a show not started yet. */
    public record MovieAudience(Set<Long> watched, Set<Long> going) {}

    /** The friend (or anyone with an account) who gets the ticket, by exact email. */
    public record GiftRequest(
            @NotBlank @Email @Size(max = 255) String email) {}

    /** Split the locked seats with 1 to 9 friends. Each pays an equal share; the organizer also pays any leftover paise. */
    public record CreateSplitRequest(
            @NotBlank @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
            String lockId,

            @NotEmpty @Size(max = 9) List<@NotNull @Positive Long> friendIds) {}

    /** One person's share. Pay it in Razorpay checkout with razorpayOrderId, then confirm it. */
    public record ShareResponse(
            Long id,
            Long bookingId,
            Long userId,
            BigDecimal amount,
            long amountInPaise,
            String currency,
            String status,
            String razorpayOrderId,
            String razorpayKeyId) {}

    /** The split booking stays PENDING_PAYMENT until every share is PAID, or expires at expiresAt. */
    public record SplitResponse(
            Long bookingId, String status, BigDecimal total, Instant expiresAt, List<ShareResponse> shares) {}

    public record CheckInRequest(@NotBlank @Size(max = 100) String ticketCode) {}
}
