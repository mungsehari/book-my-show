package com.bookmyshow.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** The logged-in user, read from a verified access token. */
public record CurrentUser(long id, String role) {

    public static final String ROLE_CLAIM = "role";

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.parseLong(jwt.getSubject()), jwt.getClaimAsString(ROLE_CLAIM));
    }

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }

    public boolean isTheatreAdmin() {
        return Roles.THEATRE_ADMIN.equals(role);
    }
}
