package com.bookmyshow.common.error;

/** Thrown when a logged-in user may not act on a resource. Returns 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
