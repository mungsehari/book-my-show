package com.bookmyshow.modules.booking;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.booking.BookingTypes.CreateSplitRequest;
import com.bookmyshow.modules.booking.BookingTypes.ShareResponse;
import com.bookmyshow.modules.booking.BookingTypes.SplitResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Split payment")
@SecurityRequirement(name = "bearer")
public class SplitController {

    private final SplitService splitService;

    public SplitController(SplitService splitService) {
        this.splitService = splitService;
    }

    @PostMapping("/api/v1/bookings/split")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Split locked seats with friends. Each gets a share to pay within 15 minutes.")
    public SplitResponse create(@Valid @RequestBody CreateSplitRequest request, @AuthenticationPrincipal Jwt jwt) {
        return splitService.create(request.lockId(), request.friendIds(), CurrentUser.from(jwt));
    }

    @GetMapping("/api/v1/payment-shares/me")
    @Operation(summary = "Your unpaid shares")
    public ListResponse<ShareResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return ListResponse.of(splitService.myPendingShares(CurrentUser.from(jwt)));
    }

    @PostMapping("/api/v1/payment-shares/{shareId}/confirm")
    @Operation(summary = "Confirm your share with what Razorpay checkout returned")
    public ShareResponse confirm(
            @PathVariable @Positive long shareId,
            @Valid @RequestBody ConfirmPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return splitService.confirm(shareId, request, CurrentUser.from(jwt));
    }
}
