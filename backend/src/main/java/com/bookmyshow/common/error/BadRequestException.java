package com.bookmyshow.common.error;

/** Thrown when a request breaks a business rule. Returns 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
