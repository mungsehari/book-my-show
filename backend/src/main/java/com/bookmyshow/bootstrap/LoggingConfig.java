package com.bookmyshow.bootstrap;

import org.springframework.context.annotation.Bean;

/** Request logging. Log format and files are set in {@code logback-spring.xml}. */
public class LoggingConfig {

    @Bean
    RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
}
