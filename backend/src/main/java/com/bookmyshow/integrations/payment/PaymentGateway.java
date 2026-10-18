package com.bookmyshow.integrations.payment;

/** Takes payments. Modules use this interface only, never a vendor SDK. */
public interface PaymentGateway {

    /**
     * Creates an order the frontend can pay.
     *
     * @param amountInPaise amount in the smallest unit, for example 45000 for 450.00 INR
     * @throws com.bookmyshow.common.error.BadRequestException if payments are not set up
     * @throws com.bookmyshow.common.error.UpstreamException if the provider fails
     */
    PaymentOrder createOrder(long amountInPaise, String currency, String receipt);

    /**
     * Refunds money for a captured payment and returns the refund ID.
     *
     * @throws com.bookmyshow.common.error.UpstreamException if the provider fails
     */
    String refund(String paymentId, long amountInPaise, String note);

    /** True if the signature the frontend got after paying is genuine for this order and payment. */
    boolean isValidPaymentSignature(String orderId, String paymentId, String signature);

    /** True if a webhook body really came from the provider. */
    boolean isValidWebhookSignature(String body, String signature);

    /** Public key the frontend needs to open checkout. */
    String publicKey();

    record PaymentOrder(String id, long amount, String currency) {}
}
