package com.bookmyshow.modules.booking;

import com.bookmyshow.modules.booking.BookingTypes.CheckInRequest;
import com.bookmyshow.modules.booking.BookingTypes.TicketResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Tickets")
@SecurityRequirement(name = "bearer")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/api/v1/bookings/{bookingId}/ticket")
    @Operation(summary = "Ticket for a confirmed booking. Show ticketCode as a QR code at the entry.")
    public TicketResponse getTicket(@PathVariable @Positive long bookingId, @AuthenticationPrincipal Jwt jwt) {
        return ticketService.getTicket(bookingId, CurrentUser.from(jwt));
    }

    @PostMapping("/api/v1/admin/tickets/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'THEATRE_ADMIN')")
    @Operation(summary = "Scan a ticket at the entry. Each ticket works once, on the show day.")
    public TicketResponse checkIn(@Valid @RequestBody CheckInRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ticketService.checkIn(request.ticketCode(), CurrentUser.from(jwt));
    }
}
