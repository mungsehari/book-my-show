package com.bookmyshow.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Single entry point for all startup setup.
 *
 * <p>Each concern lives in its own class in this package. To add one, create the class and add it
 * to the list below. The classes in the list are plain classes, so nothing loads unless it is listed
 * here.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
@Import({
    ClockConfig.class,
    CorsConfig.class,
    DatabaseConfig.class,
    LoggingConfig.class,
    SecurityHeadersConfig.class,
    JwtConfig.class,
    SecurityConfig.class,
    TelemetryConfig.class,
    OpenApiConfig.class
})
public class Bootstrap {}
