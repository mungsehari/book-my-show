package com.bookmyshow.security;

/** User roles. Stored as text in users.role and sent as the "role" claim in access tokens. */
public final class Roles {

    public static final String USER = "USER";
    public static final String THEATRE_ADMIN = "THEATRE_ADMIN";
    public static final String ADMIN = "ADMIN";

    private Roles() {}
}
