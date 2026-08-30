package com.digitalbank.authservice.application.port.in;

import com.digitalbank.authservice.domain.model.SessionId;

public record SessionValidationResult(String username, SessionId sessionId) {}
