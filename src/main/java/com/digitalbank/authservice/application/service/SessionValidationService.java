package com.digitalbank.authservice.application.service;

import com.digitalbank.authservice.application.port.in.SessionValidationResult;
import com.digitalbank.authservice.application.port.in.ValidateSessionCommand;
import com.digitalbank.authservice.application.port.in.ValidateSessionInputPort;
import com.digitalbank.authservice.application.port.out.JwtClaims;
import com.digitalbank.authservice.application.port.out.JwtTokenPort;
import com.digitalbank.authservice.application.port.out.SessionRepository;
import com.digitalbank.authservice.domain.exception.AuthenticationFailedException;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SessionValidationService implements ValidateSessionInputPort {

    private final SessionRepository sessionRepository;
    private final JwtTokenPort jwtTokenPort;
    private final Clock clock;

    public SessionValidationService(SessionRepository sessionRepository, JwtTokenPort jwtTokenPort, Clock clock) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.jwtTokenPort = Objects.requireNonNull(jwtTokenPort);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public SessionValidationResult validate(ValidateSessionCommand command) {
        var claims = jwtTokenPort.verify(command.accessToken());
        var session = sessionRepository
                .findById(claims.sessionId())
                .filter(value -> isValid(claims, value.username(), value.isActiveAt(clock.instant())))
                .orElseThrow(AuthenticationFailedException::new);
        return new SessionValidationResult(session.username(), session.id());
    }

    private boolean isValid(JwtClaims claims, String username, boolean active) {
        return active && username.equals(claims.subject()) && claims.expiresAt().isAfter(clock.instant());
    }
}
