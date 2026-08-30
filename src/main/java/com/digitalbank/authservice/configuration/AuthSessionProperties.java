package com.digitalbank.authservice.configuration;

import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.session")
public record AuthSessionProperties(SingleSessionPolicy policy, Duration ttl) {

    public AuthSessionProperties {
        if (policy == null) {
            throw new IllegalArgumentException("auth.session.policy must be configured");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("auth.session.ttl must be positive");
        }
    }
}
