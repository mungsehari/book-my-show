package com.bookmyshow.integrations.google;

/** Checks a Google ID token from the frontend and returns who signed in. */
public interface GoogleTokenVerifier {

    /**
     * Returns the Google user for a valid token.
     *
     * @throws com.bookmyshow.common.error.UnauthorizedException if the token is invalid or expired
     * @throws com.bookmyshow.common.error.BadRequestException if Google sign-in is not set up
     */
    GoogleUser verify(String idToken);

    record GoogleUser(String subject, String email, boolean emailVerified, String name) {}
}
