package com.bookmyshow.modules.booking;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ResaleListing;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Resale")
public class ResaleController {

    private final ResaleService resaleService;

    public ResaleController(ResaleService resaleService) {
        this.resaleService = resaleService;
    }

    @GetMapping("/api/v1/shows/{showId}/resale")
    @Operation(summary = "Resale tickets for a show, at face value")
    public ListResponse<ResaleListing> listings(@PathVariable @Positive long showId) {
        return ListResponse.of(resaleService.listings(showId));
    }

    @PostMapping("/api/v1/bookings/{bookingId}/resale")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Offer your whole ticket for resale at the price you paid")
    public BookingResponse list(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return resaleService.list(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/bookings/{bookingId}/resale/cancel")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Stop offering your ticket for resale")
    public BookingResponse unlist(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return resaleService.unlist(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/resale/{bookingId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Start paying for a resale ticket. Confirm with POST /bookings/{id}/confirm as usual.")
    public CheckoutResponse checkout(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return resaleService.checkout(bookingId, CurrentUser.from(jwt));
    }
}
