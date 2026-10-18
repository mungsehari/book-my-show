package com.bookmyshow.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookmyshow.integrations.ai.ClaudeProperties;
import com.bookmyshow.integrations.ai.ClaudeQueryInterpreter;
import com.bookmyshow.integrations.google.NimbusGoogleTokenVerifier;
import com.bookmyshow.integrations.payment.RazorpayGateway;
import com.bookmyshow.integrations.payment.RazorpayProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests build these classes by hand, so a missing Spring bean hides. This test lets Spring build them from their
 * real constructors, with only the beans the app itself would supply. It needs no database.
 */
class IntegrationBeansWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(HttpClientConfig.class)
            .withBean(RazorpayProperties.class, () -> new RazorpayProperties("", "", ""))
            .withBean(ClaudeProperties.class, () -> new ClaudeProperties("", null))
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(
                    AppProperties.class,
                    () -> new AppProperties(
                            "Asia/Kolkata",
                            null,
                            new AppProperties.Jwt(
                                    "test-secret-that-is-at-least-32-bytes-long", Duration.ofHours(1), "x"),
                            List.of(),
                            null,
                            null));

    @Test
    void razorpayGatewayIsCreated() {
        runner.withUserConfiguration(RazorpayGateway.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RazorpayGateway.class);
        });
    }

    @Test
    void claudeInterpreterIsCreated() {
        runner.withUserConfiguration(ClaudeQueryInterpreter.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ClaudeQueryInterpreter.class).isAvailable())
                    .isFalse();
        });
    }

    @Test
    void googleVerifierIsCreated() {
        runner.withUserConfiguration(NimbusGoogleTokenVerifier.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(NimbusGoogleTokenVerifier.class);
        });
    }

    @Test
    void eachGatewayGetsItsOwnBuilder() {
        runner.run(context -> assertThat(context.getBean(org.springframework.web.client.RestClient.Builder.class))
                .isNotSameAs(context.getBean(org.springframework.web.client.RestClient.Builder.class)));
    }
}
