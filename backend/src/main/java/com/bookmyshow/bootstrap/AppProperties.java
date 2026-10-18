package com.bookmyshow.bootstrap;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** App settings from the {@code app.*} keys in application.yml. */
@ConfigurationProperties("app")
public record AppProperties(String timezone, Cors cors) {

    public record Cors(List<String> allowedOrigins) {}
}
