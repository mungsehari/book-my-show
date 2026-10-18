package com.bookmyshow.modules.booking;

import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Bookings")
@SecurityRequirement(name = "bearer")
public class BookingAdminController {

    private final RefundService refundService;

    public BookingAdminController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/api/v1/admin/bookings/{bookingId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a booking that was paid after its seats were sold (NEEDS_REFUND)")
    public BookingResponse refund(@PathVariable @Positive long bookingId) {
        return refundService.refundNeedsRefund(bookingId);
    }
}
