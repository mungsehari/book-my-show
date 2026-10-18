package com.bookmyshow.integrations.google;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bookmyshow.bootstrap.AppProperties;
import com.bookmyshow.common.error.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class NimbusGoogleTokenVerifierTest {

    @Test
    void isOffWithoutClientId() {
        AppProperties properties =
                new AppProperties("Asia/Kolkata", null, null, List.of(), null, new AppProperties.Google(" "));
        NimbusGoogleTokenVerifier verifier = new NimbusGoogleTokenVerifier(properties);

        assertThatThrownBy(() -> verifier.verify("any-token"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Google sign-in is not set up");
    }
}
