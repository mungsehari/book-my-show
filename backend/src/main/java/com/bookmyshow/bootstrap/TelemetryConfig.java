package com.bookmyshow.bootstrap;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Metrics and health. Endpoints are exposed through Actuator ({@code management.*} in
 * application.yml). Tracing comes later.
 */
public class TelemetryConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> commonMetricTags(@Value("${spring.application.name}") String appName) {
        return registry -> registry.config().commonTags("application", appName);
    }
}
