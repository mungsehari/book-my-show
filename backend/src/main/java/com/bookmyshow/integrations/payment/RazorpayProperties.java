package com.bookmyshow.integrations.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Razorpay keys from the {@code app.razorpay.*} keys, which come from environment variables. */
@ConfigurationProperties("app.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret) {

    public boolean isConfigured() {
        return notBlank(keyId) && notBlank(keySecret);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
