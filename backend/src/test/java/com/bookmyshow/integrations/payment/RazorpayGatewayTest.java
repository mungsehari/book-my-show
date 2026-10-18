package com.bookmyshow.integrations.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookmyshow.common.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RazorpayGatewayTest {

    private static final RazorpayProperties TEST_KEYS =
            new RazorpayProperties("rzp_test_abc", "test-key-secret", "test-webhook-secret");

    @Test
    void hmacMatchesKnownValue() {
        // Standard HMAC SHA-256 example.
        assertThat(RazorpayGateway.hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog"))
                .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void acceptsGenuinePaymentSignature() {
        RazorpayGateway gateway = new RazorpayGateway(TEST_KEYS, RestClient.builder());
        String signature = RazorpayGateway.hmacSha256Hex("test-key-secret", "order_1|pay_1");

        assertThat(gateway.isValidPaymentSignature("order_1", "pay_1", signature))
                .isTrue();
        assertThat(gateway.isValidPaymentSignature("order_1", "pay_2", signature))
                .isFalse();
        assertThat(gateway.isValidPaymentSignature("order_1", "pay_1", null)).isFalse();
    }

    @Test
    void checksWebhookBodyWithWebhookSecret() {
        RazorpayGateway gateway = new RazorpayGateway(TEST_KEYS, RestClient.builder());
        String body = "{\"event\":\"payment.captured\"}";

        assertThat(gateway.isValidWebhookSignature(body, RazorpayGateway.hmacSha256Hex("test-webhook-secret", body)))
                .isTrue();
        assertThat(gateway.isValidWebhookSignature(body, RazorpayGateway.hmacSha256Hex("test-key-secret", body)))
                .isFalse();
    }

    @Test
    void refusesLiveKeys() {
        RazorpayProperties live = new RazorpayProperties("rzp_live_abc", "secret", "hook");

        assertThatThrownBy(() -> new RazorpayGateway(live, RestClient.builder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("live key");
    }

    @Test
    void isOffWithoutKeys() {
        RazorpayGateway gateway = new RazorpayGateway(new RazorpayProperties("", "", ""), RestClient.builder());

        assertThatThrownBy(() -> gateway.createOrder(45000, "INR", "booking-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Payments are not set up");
        assertThat(gateway.isValidPaymentSignature("o", "p", "s")).isFalse();
        assertThat(gateway.isValidWebhookSignature("{}", "s")).isFalse();
    }
}
