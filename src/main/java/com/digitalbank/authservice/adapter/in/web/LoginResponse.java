package com.digitalbank.authservice.adapter.in.web;

import com.digitalbank.authservice.application.port.in.LoginResult;
import java.time.Instant;
import java.util.UUID;

record LoginResponse(String accessToken, String tokenType, UUID sessionId, Instant expiresAt) {

    static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.accessToken(), "Bearer", result.sessionId().value(), result.expiresAt());
    }
}
