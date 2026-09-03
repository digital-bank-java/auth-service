package com.digitalbank.authservice.application.service;

import com.digitalbank.authservice.application.port.in.LoginCommand;
import com.digitalbank.authservice.application.port.in.LoginInputPort;
import com.digitalbank.authservice.application.port.in.LoginResult;
import com.digitalbank.authservice.application.port.in.LogoutCommand;
import com.digitalbank.authservice.application.port.in.LogoutInputPort;
import com.digitalbank.authservice.application.port.out.CredentialStore;
import com.digitalbank.authservice.application.port.out.JwtTokenPort;
import com.digitalbank.authservice.application.port.out.PasswordHasher;
import com.digitalbank.authservice.application.port.out.SessionRepository;
import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.configuration.AuthSessionProperties;
import com.digitalbank.authservice.domain.exception.AuthenticationFailedException;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements LoginInputPort, LogoutInputPort {

    private final CredentialStore credentialStore;
    private final PasswordHasher passwordHasher;
    private final SessionRepository sessionRepository;
    private final JwtTokenPort jwtTokenPort;
    private final Clock clock;
    private final Duration sessionTtl;
    private final SingleSessionPolicy singleSessionPolicy;

    public AuthenticationService(
            CredentialStore credentialStore,
            PasswordHasher passwordHasher,
            SessionRepository sessionRepository,
            JwtTokenPort jwtTokenPort,
            Clock clock,
            AuthSessionProperties sessionProperties) {
        this.credentialStore = Objects.requireNonNull(credentialStore);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
        this.jwtTokenPort = Objects.requireNonNull(jwtTokenPort);
        this.clock = Objects.requireNonNull(clock);
        var properties = Objects.requireNonNull(sessionProperties);
        this.sessionTtl = properties.ttl();
        this.singleSessionPolicy = properties.policy();
    }

    @Override
    public LoginResult login(LoginCommand command) {
        var credential = credentialStore
                .findByUsername(command.username())
                .filter(value -> passwordHasher.matches(command.password(), value.passwordHash()))
                .orElseThrow(AuthenticationFailedException::new);

        var createdAt = clock.instant();
        var session = Session.start(SessionId.newId(), credential.username(), createdAt, createdAt.plus(sessionTtl));
        var openedSession = sessionRepository.open(session, singleSessionPolicy);
        var accessToken = jwtTokenPort.issue(credential.username(), openedSession);
        return new LoginResult(accessToken, openedSession.id(), openedSession.expiresAt());
    }

    @Override
    public void logout(LogoutCommand command) {
        var claims = jwtTokenPort.verify(command.accessToken());
        sessionRepository.revoke(claims.sessionId());
    }
}
