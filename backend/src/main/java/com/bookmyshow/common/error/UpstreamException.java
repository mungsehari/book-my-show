package com.bookmyshow.common.error;

/** Thrown when a third-party service (like the payment provider) fails. Returns 502. */
public class UpstreamException extends RuntimeException {

    public UpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
