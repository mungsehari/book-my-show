package com.bookmyshow.common.error;

/** Thrown when login details or a code are wrong. Returns 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
