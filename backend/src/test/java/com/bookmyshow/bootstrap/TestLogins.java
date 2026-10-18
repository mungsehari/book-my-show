package com.bookmyshow.bootstrap;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Logged-in requests for MockMvc tests, shaped like the real access tokens. */
public final class TestLogins {

    private TestLogins() {}

    public static RequestPostProcessor as(long userId, String role) {
        return jwt().jwt(token -> token.subject(Long.toString(userId)).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
