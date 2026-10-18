package com.bookmyshow.bootstrap;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;

/** App clock in the configured time zone. Inject {@link Clock} instead of calling now() directly. */
public class ClockConfig {

    @Bean
    Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
