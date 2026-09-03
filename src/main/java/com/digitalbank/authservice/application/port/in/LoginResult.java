package com.digitalbank.authservice.application.port.in;

import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;

public record LoginResult(String accessToken, SessionId sessionId, Instant expiresAt) {}
