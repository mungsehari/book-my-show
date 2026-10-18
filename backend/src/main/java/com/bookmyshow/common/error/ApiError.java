package com.bookmyshow.common.error;

import java.time.Instant;

/** Common error body for every failed request. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, String requestId) {}
