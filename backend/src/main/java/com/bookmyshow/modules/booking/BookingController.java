package com.bookmyshow.modules.booking;

import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.booking.BookingTypes.CreateBookingRequest;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings")
@SecurityRequirement(name = "bearer")
public class BookingController {

    private final BookingService bookingService;
    private final RefundService refundService;

    public BookingController(BookingService bookingService, RefundService refundService) {
        this.bookingService = bookingService;
        this.refundService = refundService;
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel with a full refund, up to 2 hours before the show")
    public BookingResponse cancel(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return refundService.cancel(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start payment for locked seats. Returns the Razorpay order for checkout.")
    public CheckoutResponse checkout(
            @Valid @RequestBody CreateBookingRequest request, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.checkout(request.lockId(), CurrentUser.from(jwt));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm with the values Razorpay checkout returned after payment")
    public BookingResponse confirm(
            @PathVariable @Positive long bookingId,
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return bookingService.confirm(bookingId, request, CurrentUser.from(jwt));
    }

    @GetMapping("/me")
    @Operation(summary = "Your bookings, upcoming (soonest first) or past (newest first)")
    public PageResponse<BookingResponse> listMine(
            @Parameter(description = "upcoming or past")
                    @RequestParam(defaultValue = "upcoming")
                    @Pattern(regexp = "^(upcoming|past)$")
                    String when,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return bookingService.listMine(CurrentUser.from(jwt), when, page, size);
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "One of your bookings")
    public BookingResponse get(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.get(bookingId, CurrentUser.from(jwt));
    }
}
