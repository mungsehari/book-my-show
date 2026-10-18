package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.UnauthorizedException;
import com.bookmyshow.integrations.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Razorpay webhooks. They confirm a booking even if the user closed the browser before the confirm call. Unknown
 * events and orders are ignored, so Razorpay does not keep retrying them.
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PaymentGateway paymentGateway;
    private final JsonMapper jsonMapper;

    public PaymentWebhookService(
            BookingRepository bookingRepository,
            BookingService bookingService,
            PaymentGateway paymentGateway,
            JsonMapper jsonMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.paymentGateway = paymentGateway;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void handle(String body, String signature) {
        if (!paymentGateway.isValidWebhookSignature(body, signature)) {
            throw new UnauthorizedException("Invalid webhook signature");
        }
        JsonNode root;
        try {
            root = jsonMapper.readTree(body);
        } catch (JacksonException ex) {
            throw new BadRequestException("Webhook body is not valid JSON");
        }
        String event = root.path("event").asString();
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String orderId = payment.path("order_id").asString();
        String paymentId = payment.path("id").asString();
        if (orderId.isEmpty()) {
            log.info("Ignoring Razorpay webhook {} without an order", event);
            return;
        }

        bookingRepository
                .findByRazorpayOrderIdForUpdate(orderId)
                .ifPresentOrElse(
                        booking -> apply(event, booking, paymentId),
                        () -> log.info("Ignoring Razorpay webhook {} for unknown order {}", event, orderId));
    }

    private void apply(String event, Booking booking, String paymentId) {
        switch (event) {
            case "payment.captured", "order.paid" -> bookingService.applyPayment(booking, paymentId);
            case "payment.failed" -> {
                if (BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
                    booking.markFailed();
                }
            }
            default -> log.info("Ignoring Razorpay webhook {} for booking {}", event, booking.getId());
        }
    }
}
