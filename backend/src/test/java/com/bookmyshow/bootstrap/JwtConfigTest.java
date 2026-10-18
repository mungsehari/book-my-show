package com.bookmyshow.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.JwtService;
import com.bookmyshow.security.JwtService.IssuedToken;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtConfigTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";

    private final JwtConfig config = new JwtConfig();

    @Test
    void issuedTokenDecodesWithUserAndRole() {
        AppProperties properties = properties(SECRET, "book-my-show");
        SecretKey key = config.jwtSecretKey(properties);
        JwtService jwtService = new JwtService(config.jwtEncoder(key), properties, Clock.systemUTC());

        IssuedToken token = jwtService.issue(7L, Roles.ADMIN);
        Jwt jwt = config.jwtDecoder(key, properties).decode(token.value());

        CurrentUser user = CurrentUser.from(jwt);
        assertThat(user.id()).isEqualTo(7L);
        assertThat(user.isAdmin()).isTrue();
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        SecretKey key = config.jwtSecretKey(properties(SECRET, "book-my-show"));
        JwtService otherIssuer =
                new JwtService(config.jwtEncoder(key), properties(SECRET, "someone-else"), Clock.systemUTC());
        JwtDecoder decoder = config.jwtDecoder(key, properties(SECRET, "book-my-show"));

        String token = otherIssuer.issue(7L, Roles.USER).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void failsFastOnShortSecret() {
        assertThatThrownBy(() -> config.jwtSecretKey(properties("too-short", "book-my-show")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    private static AppProperties properties(String secret, String issuer) {
        return new AppProperties(
                "Asia/Kolkata",
                null,
                new AppProperties.Jwt(secret, Duration.ofHours(1), issuer),
                List.of(),
                null,
                null);
    }
}
