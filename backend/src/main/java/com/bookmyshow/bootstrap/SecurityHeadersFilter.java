package com.bookmyshow.bootstrap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds common security headers. API routes also get a strict Content-Security-Policy. */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        // Swagger UI needs its own scripts, so the strict policy applies to the API only.
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
        }
        chain.doFilter(request, response);
    }
}
