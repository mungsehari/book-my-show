package com.bookmyshow.integrations.google;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.UnauthorizedException;
import java.util.Set;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/** Checks Google ID tokens against Google's public keys: signature, issuer, audience (our client ID), and expiry. */
@Component
public class NimbusGoogleTokenVerifier implements GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final JwtDecoder decoder;

    public NimbusGoogleTokenVerifier(AppProperties properties) {
        String configured =
                properties.google() == null ? null : properties.google().clientId();
        this.clientId = configured == null || configured.isBlank() ? null : configured.strip();
        this.decoder = clientId == null ? null : buildDecoder(clientId);
    }

    @Override
    public GoogleUser verify(String idToken) {
        if (decoder == null) {
            throw new BadRequestException("Google sign-in is not set up");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException ex) {
            throw new UnauthorizedException("Invalid Google token");
        }
        return new GoogleUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")),
                jwt.getClaimAsString("name"));
    }

    private static JwtDecoder buildDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(
                        JwtClaimNames.ISS, iss -> iss != null && ISSUERS.contains(iss.toString())),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Wrong audience", null))));
        return decoder;
    }
}
