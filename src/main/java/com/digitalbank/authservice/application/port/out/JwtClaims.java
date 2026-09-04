package com.digitalbank.authservice.application.port.out;

import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;
import java.util.List;

public record JwtClaims(
        String subject,
        SessionId sessionId,
        String issuer,
        Instant issuedAt,
        Instant expiresAt,
        boolean active,
        List<String> scopes) {

    public JwtClaims(
            String subject, SessionId sessionId, String issuer, Instant issuedAt, Instant expiresAt, boolean active) {
        this(subject, sessionId, issuer, issuedAt, expiresAt, active, List.of());
    }

    public JwtClaims {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
