package com.bookmyshow.bootstrap;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

/**
 * A {@link RestClient.Builder} for outgoing HTTP calls (the Razorpay gateway). Spring Boot 4 does not create one
 * unless an extra starter is added, so it is defined here. Each user gets its own copy, because a builder is changed as
 * it is used.
 */
public class HttpClientConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
