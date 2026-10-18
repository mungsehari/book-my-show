package com.bookmyshow.bootstrap;

import com.bookmyshow.common.web.RequestId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

/** Gives every request an ID (added to logs and the response) and logs one line per request. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(RequestId.HEADER));
        long start = System.nanoTime();
        MDC.put(RequestId.MDC_KEY, requestId);
        response.setHeader(RequestId.HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), millis);
            MDC.remove(RequestId.MDC_KEY);
        }
    }

    /** Reuses a safe incoming ID, otherwise creates a new one. */
    private static String resolveRequestId(String incoming) {
        if (incoming != null && incoming.matches("[A-Za-z0-9-]{1,64}")) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
