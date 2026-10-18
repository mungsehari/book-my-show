package com.bookmyshow.bootstrap;

import org.springframework.context.annotation.Bean;

/** Security headers on every response, similar to Helmet. Spring Security comes later with auth. */
public class SecurityHeadersConfig {

    @Bean
    SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }
}
