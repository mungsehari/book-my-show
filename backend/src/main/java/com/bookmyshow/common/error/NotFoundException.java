package com.bookmyshow.common.error;

/** Thrown when a requested resource does not exist. Returns 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
