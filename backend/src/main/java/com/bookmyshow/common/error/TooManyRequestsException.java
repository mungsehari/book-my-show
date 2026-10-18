package com.bookmyshow.common.error;

/** Thrown when a client must wait before trying again, like a new OTP too soon. Returns 429. */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
