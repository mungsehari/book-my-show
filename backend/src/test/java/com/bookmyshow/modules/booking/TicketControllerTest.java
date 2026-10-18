package com.bookmyshow.modules.booking;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.modules.booking.BookingTypes.TicketResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithSecurityConfig
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    private static final TicketResponse TICKET = new TicketResponse(
            9L,
            "bms1.9.sig",
            "Sky Riders",
            "City Cineplex Andheri",
            "Screen 1",
            LocalDate.of(2026, 10, 18),
            LocalTime.of(18, 0),
            List.of("A1"),
            new BigDecimal("200.00"),
            "INR",
            null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void ownerGetsTicket() throws Exception {
        when(ticketService.getTicket(9L, new CurrentUser(7L, Roles.USER))).thenReturn(TICKET);

        mockMvc.perform(get("/api/v1/bookings/9/ticket").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("bms1.9.sig"))
                .andExpect(jsonPath("$.seats[0]").value("A1"));
    }

    @Test
    void ticketNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/9/ticket")).andExpect(status().isUnauthorized());
    }

    @Test
    void staffChecksIn() throws Exception {
        when(ticketService.checkIn("bms1.9.sig", new CurrentUser(2L, Roles.THEATRE_ADMIN)))
                .thenReturn(TICKET);

        mockMvc.perform(post("/api/v1/admin/tickets/check-in")
                        .with(TestLogins.as(2L, Roles.THEATRE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\": \"bms1.9.sig\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(9));
    }

    @Test
    void secondScanIs409() throws Exception {
        when(ticketService.checkIn("bms1.9.sig", new CurrentUser(2L, Roles.THEATRE_ADMIN)))
                .thenThrow(new ConflictException("Ticket was already used at 2026-10-18T11:30:00Z"));

        mockMvc.perform(post("/api/v1/admin/tickets/check-in")
                        .with(TestLogins.as(2L, Roles.THEATRE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\": \"bms1.9.sig\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void plainUserCannotCheckIn() throws Exception {
        mockMvc.perform(post("/api/v1/admin/tickets/check-in")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\": \"bms1.9.sig\"}"))
                .andExpect(status().isForbidden());
    }
}
