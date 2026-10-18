package com.bookmyshow.modules.booking;

import com.bookmyshow.bootstrap.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Signed ticket codes like {@code bms1.42.Xk9...}: a version, the booking ID, and an HMAC SHA-256 signature cut to 16
 * bytes. Short, so the QR code stays easy to scan.
 *
 * <p>The key is derived from JWT_SECRET with a fixed label, so no new secret is needed, and a ticket code can never pass
 * as an access token (different key, different format).
 */
@Component
class TicketCodes {

    private static final String PREFIX = "bms1";
    private static final int SIGNATURE_BYTES = 16;

    private final byte[] key;

    TicketCodes(AppProperties properties) {
        this.key = hmac(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "ticket-code-key-v1");
    }

    String issue(long bookingId) {
        String body = PREFIX + "." + bookingId;
        return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(body));
    }

    /** The booking ID if the code is genuine, else empty. */
    Optional<Long> verify(String code) {
        String[] parts = code == null ? new String[0] : code.strip().split("\\.");
        if (parts.length != 3 || !PREFIX.equals(parts[0]) || !parts[1].matches("^[1-9][0-9]{0,18}$")) {
            return Optional.empty();
        }
        byte[] given;
        try {
            given = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        byte[] expected = sign(PREFIX + "." + parts[1]);
        return MessageDigest.isEqual(expected, given) ? Optional.of(Long.parseLong(parts[1])) : Optional.empty();
    }

    private byte[] sign(String body) {
        return Arrays.copyOf(hmac(key, body), SIGNATURE_BYTES);
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC SHA-256 is not available", ex);
        }
    }
}
