package com.digitalbank.authservice.adapter.in.web;

import com.digitalbank.authservice.application.port.in.SessionValidationResult;
import java.util.UUID;

record SessionValidationResponse(String username, UUID sessionId) {

    static SessionValidationResponse from(SessionValidationResult result) {
        return new SessionValidationResponse(
                result.username(), result.sessionId().value());
    }
}
