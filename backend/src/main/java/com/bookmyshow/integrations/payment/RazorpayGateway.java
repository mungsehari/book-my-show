package com.bookmyshow.integrations.payment;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.UpstreamException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Razorpay over its REST API, without the SDK. Test keys only: the app refuses to start with a live key, so no real
 * money can move from this code.
 */
@Component
public class RazorpayGateway implements PaymentGateway {

    private static final String API = "https://api.razorpay.com/v1";

    private final RazorpayProperties properties;
    private final RestClient restClient;

    public RazorpayGateway(RazorpayProperties properties, RestClient.Builder builder) {
        if (properties.keyId() != null && properties.keyId().startsWith("rzp_live_")) {
            throw new IllegalStateException("RAZORPAY_KEY_ID is a live key. Only test keys (rzp_test_) are allowed.");
        }
        this.properties = properties;
        this.restClient = properties.isConfigured()
                ? builder.baseUrl(API)
                        .defaultHeaders(headers -> headers.setBasicAuth(properties.keyId(), properties.keySecret()))
                        .build()
                : null;
    }

    @Override
    public PaymentOrder createOrder(long amountInPaise, String currency, String receipt) {
        if (restClient == null) {
            throw new BadRequestException("Payments are not set up");
        }
        try {
            RazorpayOrder order = restClient
                    .post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountInPaise, "currency", currency, "receipt", receipt))
                    .retrieve()
                    .body(RazorpayOrder.class);
            if (order == null || order.id() == null) {
                throw new UpstreamException("Razorpay returned no order", null);
            }
            return new PaymentOrder(order.id(), order.amount(), order.currency());
        } catch (RestClientException ex) {
            throw new UpstreamException("Could not create the payment order. Try again.", ex);
        }
    }

    @Override
    public String refund(String paymentId, long amountInPaise, String note) {
        if (restClient == null) {
            throw new BadRequestException("Payments are not set up");
        }
        try {
            RazorpayRefund refund = restClient
                    .post()
                    .uri("/payments/{paymentId}/refund", paymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", amountInPaise, "notes", Map.of("reason", note)))
                    .retrieve()
                    .body(RazorpayRefund.class);
            if (refund == null || refund.id() == null) {
                throw new UpstreamException("Razorpay returned no refund", null);
            }
            return refund.id();
        } catch (RestClientException ex) {
            throw new UpstreamException("Could not refund the payment. Try again.", ex);
        }
    }

    /** Razorpay signs "orderId|paymentId" with the key secret (HMAC SHA-256, hex). */
    @Override
    public boolean isValidPaymentSignature(String orderId, String paymentId, String signature) {
        return properties.isConfigured()
                && matches(hmacSha256Hex(properties.keySecret(), orderId + "|" + paymentId), signature);
    }

    /** Razorpay signs the raw webhook body with the webhook secret (HMAC SHA-256, hex). */
    @Override
    public boolean isValidWebhookSignature(String body, String signature) {
        String secret = properties.webhookSecret();
        return secret != null && !secret.isBlank() && matches(hmacSha256Hex(secret, body), signature);
    }

    @Override
    public String publicKey() {
        return properties.keyId();
    }

    static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC SHA-256 is not available", ex);
        }
    }

    /** Constant-time compare, so the check does not leak how much of the signature was right. */
    private static boolean matches(String expected, String actual) {
        return actual != null
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private record RazorpayOrder(String id, long amount, String currency) {}

    private record RazorpayRefund(String id) {}
}
