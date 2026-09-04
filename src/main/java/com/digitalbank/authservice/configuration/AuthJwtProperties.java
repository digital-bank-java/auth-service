package com.digitalbank.authservice.configuration;

import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.jwt")
public record AuthJwtProperties(String secret, String issuer, List<String> scopes) {
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
    }
}
