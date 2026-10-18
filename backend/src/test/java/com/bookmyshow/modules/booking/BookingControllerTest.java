package com.bookmyshow.modules.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookmyshow.bootstrap.TestLogins;
import com.bookmyshow.bootstrap.WithSecurityConfig;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.common.response.PageResponse;
import com.bookmyshow.modules.booking.BookingTypes.BookingResponse;
import com.bookmyshow.modules.booking.BookingTypes.CheckoutResponse;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.math.BigDecimal;
import java.time.Instant;
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
@WebMvcTest({BookingController.class, PaymentWebhookController.class})
class BookingControllerTest {

    private static final String LOCK_ID = "3f2b8c1e-7a4d-4e5f-9b6a-1c2d3e4f5a6b";
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);
    private static final BookingResponse CONFIRMED_BOOKING = new BookingResponse(
            9L,
            50L,
            "Sky Riders",
            10L,
            "Screen 1",
            LocalDate.of(2026, 10, 20),
            LocalTime.of(18, 0),
            List.of(1L),
            List.of("A1"),
            BookingStatus.CONFIRMED,
            new BigDecimal("200.00"),
            "INR",
            Instant.parse("2026-10-18T05:00:00Z"),
            Instant.parse("2026-10-18T05:02:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private PaymentWebhookService webhookService;

    @MockitoBean
    private RefundService refundService;

    @Test
    void ownerCancels() throws Exception {
        when(refundService.cancel(9L, ASHA)).thenReturn(CONFIRMED_BOOKING);

        mockMvc.perform(post("/api/v1/bookings/9/cancel").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isOk());
    }

    @Test
    void cancelNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/9/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutReturnsRazorpayOrder() throws Exception {
        when(bookingService.checkout(LOCK_ID, ASHA))
                .thenReturn(new CheckoutResponse(
                        9L,
                        BookingStatus.PENDING_PAYMENT,
                        new BigDecimal("650.00"),
                        65000,
                        "INR",
                        "order_1",
                        "rzp_test_abc"));

        mockMvc.perform(post("/api/v1/bookings")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockId\": \"" + LOCK_ID + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razorpayOrderId").value("order_1"))
                .andExpect(jsonPath("$.amountInPaise").value(65000));
    }

    @Test
    void checkoutNeedsLogin() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockId\": \"" + LOCK_ID + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkoutRejectsBadLockId() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lockId\": \"nope\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmReturnsBooking() throws Exception {
        when(bookingService.confirm(eq(9L), any(ConfirmPaymentRequest.class), eq(ASHA)))
                .thenReturn(CONFIRMED_BOOKING);

        mockMvc.perform(post("/api/v1/bookings/9/confirm")
                        .with(TestLogins.as(7L, Roles.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razorpayOrderId": "order_1", "razorpayPaymentId": "pay_1", "razorpaySignature": "sig"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void listsMyUpcomingBookings() throws Exception {
        when(bookingService.listMine(ASHA, "upcoming", 0, 20))
                .thenReturn(new PageResponse<>(List.of(CONFIRMED_BOOKING), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/bookings/me").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].seatLabels[0]").value("A1"))
                .andExpect(jsonPath("$.items[0].movieName").value("Sky Riders"));
    }

    @Test
    void historyRejectsUnknownFilterAndHugePages() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/me").param("when", "soon").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/bookings/me").param("size", "500").with(TestLogins.as(7L, Roles.USER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/9")).andExpect(status().isUnauthorized());
    }

    @Test
    void webhookIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                        .header("X-Razorpay-Signature", "sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\": \"payment.captured\"}"))
                .andExpect(status().isOk());

        verify(webhookService).handle("{\"event\": \"payment.captured\"}", "sig");
    }

    @Test
    void webhookWithBadSignatureIs401() throws Exception {
        doThrow(new UnauthorizedException("Invalid webhook signature"))
                .when(webhookService)
                .handle(anyString(), eq("bad"));

        mockMvc.perform(post("/api/v1/payments/razorpay/webhook")
                        .header("X-Razorpay-Signature", "bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
