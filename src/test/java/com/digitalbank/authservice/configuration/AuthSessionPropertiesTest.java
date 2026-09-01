package com.digitalbank.authservice.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthSessionPropertiesTest {

    @Test
    void missingPolicyDefaultsToConservativePreviousSessionRevocation() {
        var properties = new AuthSessionProperties(null, Duration.ofMinutes(30));

        assertThat(properties.policy())
                .isEqualTo(com.digitalbank.authservice.application.port.out.SingleSessionPolicy.REVOKE_PREVIOUS);
    }
}
