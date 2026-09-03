package com.digitalbank.authservice.application.port.out;

import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;

public record JwtClaims(
        String subject, SessionId sessionId, String issuer, Instant issuedAt, Instant expiresAt, boolean active) {}
