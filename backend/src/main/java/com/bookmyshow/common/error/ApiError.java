package com.bookmyshow.common.error;

import com.bookmyshow.common.web.RequestId;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

/** Common error body for every failed request. */
public record ApiError(Instant timestamp, int status, String error, String message, String path, String requestId) {

    /** Builds the body for a request. Used by the exception handler and the security error handlers. */
    public static ApiError of(HttpStatus status, String message, HttpServletRequest request) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                MDC.get(RequestId.MDC_KEY));
    }
}
