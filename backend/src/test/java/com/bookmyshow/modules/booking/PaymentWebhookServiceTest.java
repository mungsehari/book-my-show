package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.integrations.payment.PaymentGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PaymentWebhookServiceTest {

    private BookingRepository bookingRepository;
    private BookingService bookingService;
    private PaymentGateway paymentGateway;
    private PaymentWebhookService webhookService;
    private PaymentShareRepository shareRepository;
    private SplitService splitService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        bookingService = mock(BookingService.class);
        paymentGateway = mock(PaymentGateway.class);
        when(paymentGateway.isValidWebhookSignature(anyString(), anyString())).thenReturn(true);
        shareRepository = mock(PaymentShareRepository.class);
        splitService = mock(SplitService.class);
        webhookService = new PaymentWebhookService(
                bookingRepository,
                bookingService,
                shareRepository,
                splitService,
                paymentGateway,
                JsonMapper.builder().build());
    }

    @Test
    void capturedShareGoesToSplitService() {
        PaymentShare share = new PaymentShare(9L, 8L, new BigDecimal("100.00"), "INR", Instant.now());
        when(bookingRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.empty());
        when(shareRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(share));

        webhookService.handle(body("payment.captured"), "sig");

        verify(splitService).applyPayment(share, "pay_1");
    }

    @Test
    void capturedPaymentConfirmsBooking() {
        Booking booking = BookingServiceTest.pending();
        when(bookingRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(booking));

        webhookService.handle(body("payment.captured"), "sig");

        verify(bookingService).applyPayment(booking, "pay_1");
    }

    @Test
    void failedPaymentMarksBookingFailed() {
        Booking booking = BookingServiceTest.pending();
        when(bookingRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.of(booking));

        webhookService.handle(body("payment.failed"), "sig");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.FAILED);
        verify(bookingService, never()).applyPayment(booking, "pay_1");
    }

    @Test
    void unknownOrderIsIgnored() {
        when(bookingRepository.findByRazorpayOrderIdForUpdate("order_1")).thenReturn(Optional.empty());

        webhookService.handle(body("payment.captured"), "sig");

        verifyNoInteractions(bookingService);
    }

    @Test
    void rejectsBadSignature() {
        when(paymentGateway.isValidWebhookSignature("{}", "bad")).thenReturn(false);

        assertThatThrownBy(() -> webhookService.handle("{}", "bad")).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void rejectsBrokenJson() {
        assertThatThrownBy(() -> webhookService.handle("{not json", "sig")).isInstanceOf(BadRequestException.class);
    }

    private static String body(String event) {
        return """
                {"event": "%s", "payload": {"payment": {"entity": {"id": "pay_1", "order_id": "order_1"}}}}
                """.formatted(event);
    }
}
