package com.bookmyshow.modules.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Called by Razorpay, not by users. No login: the body signature proves the sender. */
@RestController
@Tag(name = "Payments")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    public PaymentWebhookController(PaymentWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/api/v1/payments/razorpay/webhook")
    @Operation(summary = "Razorpay webhook (payment.captured, order.paid, payment.failed)")
    public void handle(
            @RequestBody String body,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        webhookService.handle(body, signature);
    }
}
