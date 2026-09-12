package com.digitalbank.authservice.configuration;

import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "auth.jwt")
public record AuthJwtProperties(
        String secret, String issuer, List<String> scopes, List<String> audiences, String tokenPurpose) {
    private static final List<String> TEST_AUDIENCES = List.of(
            "api-gateway",
            "customer-service",
            "account-service",
            "ledger-service",
            "transaction-service",
            "payment-service",
            "mfa-service");

    public AuthJwtProperties(String secret, String issuer, List<String> scopes) {
        this(secret, issuer, scopes, TEST_AUDIENCES, "user-access");
    }

    @ConstructorBinding
    public AuthJwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("auth.jwt.secret must be configured");
        }
        try {
            if (Base64.getDecoder().decode(secret).length < 32) {
                throw new IllegalArgumentException("auth.jwt.secret must decode to at least 32 bytes");
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "auth.jwt.secret must be valid base64 with at least 32 bytes", exception);
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("auth.jwt.issuer must be configured");
        }
        scopes = scopes == null
                ? List.of()
                : scopes.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
        audiences = audiences == null
                ? List.of()
                : audiences.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();
        if (audiences.isEmpty()) {
            throw new IllegalArgumentException("auth.jwt.audiences must contain at least one audience");
        }
        if (tokenPurpose == null || tokenPurpose.isBlank()) {
            throw new IllegalArgumentException("auth.jwt.token-purpose must be configured");
        }
        tokenPurpose = tokenPurpose.trim();
    }
}
