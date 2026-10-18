package com.bookmyshow.bootstrap;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** App settings from the {@code app.*} keys in application.yml. Secrets come from environment variables. */
@ConfigurationProperties("app")
public record AppProperties(String timezone, Cors cors, Jwt jwt, List<String> adminEmails, Otp otp, Google google) {

    public AppProperties {
        adminEmails = adminEmails == null
                ? List.of()
                : adminEmails.stream()
                        .map(String::strip)
                        .filter(email -> !email.isEmpty())
                        .map(String::toLowerCase)
                        .toList();
    }

    public record Cors(List<String> allowedOrigins) {}

    /** Access tokens. The secret must be at least 32 bytes for HS256. */
    public record Jwt(String secret, Duration ttl, String issuer) {}

    /** Phone login codes. */
    public record Otp(Duration ttl, int maxAttempts, Duration resendAfter) {}

    /** Google sign-in. A blank client ID turns Google sign-in off. */
    public record Google(String clientId) {}
}
