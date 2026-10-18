package com.bookmyshow.security;

import com.bookmyshow.bootstrap.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues signed access tokens (HS256). Tokens are checked by the resource server setup in SecurityConfig. */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties.Jwt settings;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, AppProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.settings = properties.jwt();
        this.clock = clock;
    }

    public IssuedToken issue(long userId, String role) {
        // JWT times have whole seconds, so the expiry we return matches the one inside the token.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(settings.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(settings.issuer())
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CurrentUser.ROLE_CLAIM, role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token =
                jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
