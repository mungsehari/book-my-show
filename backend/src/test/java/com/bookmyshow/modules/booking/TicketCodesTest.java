package com.bookmyshow.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookmyshow.bootstrap.AppProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketCodesTest {

    private final TicketCodes codes = codes("test-secret-that-is-at-least-32-bytes-long");

    @Test
    void codeRoundTrips() {
        String code = codes.issue(42L);

        assertThat(code).startsWith("bms1.42.").hasSizeLessThan(40);
        assertThat(codes.verify(code)).contains(42L);
        assertThat(codes.verify(" " + code + " ")).contains(42L);
    }

    @Test
    void changedBookingIdFails() {
        String forged = codes.issue(42L).replace("bms1.42.", "bms1.43.");

        assertThat(codes.verify(forged)).isEmpty();
    }

    @Test
    void codeFromAnotherSecretFails() {
        String other = codes("another-secret-that-is-at-least-32-bytes").issue(42L);

        assertThat(codes.verify(other)).isEmpty();
    }

    @Test
    void junkFails() {
        assertThat(codes.verify(null)).isEmpty();
        assertThat(codes.verify("")).isEmpty();
        assertThat(codes.verify("bms1.42")).isEmpty();
        assertThat(codes.verify("bms2.42.abc")).isEmpty();
        assertThat(codes.verify("bms1.-1.abc")).isEmpty();
        assertThat(codes.verify("bms1.42.***")).isEmpty();
    }

    private static TicketCodes codes(String secret) {
        return new TicketCodes(new AppProperties(
                "Asia/Kolkata",
                null,
                new AppProperties.Jwt(secret, Duration.ofHours(1), "book-my-show"),
                List.of(),
                null,
                null));
    }
}
