package com.bookmyshow.common.error;

/** Thrown when a request clashes with existing data, like a duplicate or an overlapping show. Returns 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
