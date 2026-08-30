package com.digitalbank.authservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.digitalbank.authservice.adapter.out.session.InMemorySessionRepository;
import com.digitalbank.authservice.application.port.in.LoginCommand;
import com.digitalbank.authservice.application.port.in.LogoutCommand;
import com.digitalbank.authservice.application.port.in.ValidateSessionCommand;
import com.digitalbank.authservice.application.port.out.CredentialStore;
import com.digitalbank.authservice.application.port.out.JwtClaims;
import com.digitalbank.authservice.application.port.out.JwtTokenPort;
import com.digitalbank.authservice.application.port.out.PasswordHasher;
import com.digitalbank.authservice.application.port.out.SessionRepository;
import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.application.port.out.StoredCredential;
import com.digitalbank.authservice.configuration.AuthSessionProperties;
import com.digitalbank.authservice.domain.exception.AuthenticationFailedException;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void successfulLoginCreatesSessionAndReturnsToken() {
        var sessions = new RecordingSessionRepository();
        var tokens = new RecordingJwtTokenPort();
        var service = new AuthenticationService(
                username -> Optional.of(new StoredCredential(username, "bcrypt-hash")),
                (rawPassword, passwordHash) ->
                        "correct password".contentEquals(rawPassword) && passwordHash.equals("bcrypt-hash"),
                sessions,
                tokens,
                CLOCK,
                new AuthSessionProperties(SingleSessionPolicy.REVOKE_PREVIOUS, Duration.ofMinutes(30)));

        var result = service.login(new LoginCommand("alice@example.com", "correct password"));

        assertThat(result.accessToken()).isEqualTo("token");
        assertThat(result.sessionId()).isEqualTo(sessions.lastOpened.id());
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(sessions.lastPolicy).isEqualTo(SingleSessionPolicy.REVOKE_PREVIOUS);
    }

    @Test
    void unknownAndWrongPasswordsHaveTheSameFailure() {
        var service = serviceFor(username -> Optional.empty(), (rawPassword, passwordHash) -> false);

        var unknown = assertThatThrownBy(() -> service.login(new LoginCommand("unknown", "correct password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authentication failed")
                .actual();
        var wrongPassword = assertThatThrownBy(() -> service.login(new LoginCommand("alice", "wrong password")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Authentication failed")
                .actual();

        assertThat(unknown.getClass()).isEqualTo(wrongPassword.getClass());
        assertThat(unknown.getMessage()).isEqualTo(wrongPassword.getMessage());
    }

    @Test
    void logoutCanBeRepeatedWithoutFailure() {
        var sessions = new RecordingSessionRepository();
        var tokenPort = new RecordingJwtTokenPort();
        var service = serviceFor(
                username -> Optional.of(new StoredCredential(username, "hash")),
                (rawPassword, passwordHash) -> true,
                sessions,
                tokenPort);
        var session = Session.start(SessionId.newId(), "alice", NOW, NOW.plus(Duration.ofMinutes(30)));
        sessions.open(session, SingleSessionPolicy.ALLOW_MULTIPLE);
        tokenPort.issue("alice", session);

        service.logout(new LogoutCommand("token"));
        service.logout(new LogoutCommand("token"));

        assertThat(sessions.findById(session.id()).orElseThrow().status().name())
                .isEqualTo("REVOKED");
    }

    @Test
    void revokedSessionFailsValidationEvenWhenTokenIsValid() {
        var sessions = new RecordingSessionRepository();
        var tokenPort = new RecordingJwtTokenPort();
        var session = Session.start(SessionId.newId(), "alice", NOW, NOW.plus(Duration.ofMinutes(30)));
        sessions.open(session, SingleSessionPolicy.ALLOW_MULTIPLE);
        tokenPort.issue("alice", session);
        var service = new SessionValidationService(sessions, tokenPort, CLOCK);

        sessions.revoke(session.id());

        assertThatThrownBy(() -> service.validate(new ValidateSessionCommand("token")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void inactiveJwtClaimFailsValidationEvenWhenSessionIsActive() {
        var sessions = new RecordingSessionRepository();
        var tokenPort = new RecordingJwtTokenPort();
        var session = Session.start(SessionId.newId(), "alice", NOW, NOW.plus(Duration.ofMinutes(30)));
        sessions.open(session, SingleSessionPolicy.ALLOW_MULTIPLE);
        tokenPort.activeClaim = false;
        tokenPort.issue("alice", session);
        var service = new SessionValidationService(sessions, tokenPort, CLOCK);

        assertThatThrownBy(() -> service.validate(new ValidateSessionCommand("token")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void secondLoginRevokesFirstSessionAndRejectsItsToken() {
        var sessions = new InMemorySessionRepository();
        var tokens = new SessionTokenPort();
        var service = serviceFor(
                username -> Optional.of(new StoredCredential(username, "hash")),
                (rawPassword, passwordHash) -> true,
                sessions,
                tokens,
                new AuthSessionProperties(SingleSessionPolicy.REVOKE_PREVIOUS, Duration.ofMinutes(30)));
        var validation = new SessionValidationService(sessions, tokens, CLOCK);

        var first = service.login(new LoginCommand("alice", "password"));
        var second = service.login(new LoginCommand("alice", "password"));

        assertThatThrownBy(() -> validation.validate(new ValidateSessionCommand(first.accessToken())))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(validation
                        .validate(new ValidateSessionCommand(second.accessToken()))
                        .sessionId())
                .isEqualTo(second.sessionId());
    }

    @Test
    void allowMultiplePolicyKeepsBothLoginTokensValid() {
        var sessions = new InMemorySessionRepository();
        var tokens = new SessionTokenPort();
        var service = serviceFor(
                username -> Optional.of(new StoredCredential(username, "hash")),
                (rawPassword, passwordHash) -> true,
                sessions,
                tokens,
                new AuthSessionProperties(SingleSessionPolicy.ALLOW_MULTIPLE, Duration.ofMinutes(30)));
        var validation = new SessionValidationService(sessions, tokens, CLOCK);

        var first = service.login(new LoginCommand("alice", "password"));
        var second = service.login(new LoginCommand("alice", "password"));

        assertThat(validation
                        .validate(new ValidateSessionCommand(first.accessToken()))
                        .sessionId())
                .isEqualTo(first.sessionId());
        assertThat(validation
                        .validate(new ValidateSessionCommand(second.accessToken()))
                        .sessionId())
                .isEqualTo(second.sessionId());
    }

    private AuthenticationService serviceFor(CredentialStore credentials, PasswordHasher passwordHasher) {
        return serviceFor(credentials, passwordHasher, new RecordingSessionRepository(), new RecordingJwtTokenPort());
    }

    private AuthenticationService serviceFor(
            CredentialStore credentials,
            PasswordHasher passwordHasher,
            SessionRepository sessions,
            JwtTokenPort tokens) {
        return serviceFor(
                credentials,
                passwordHasher,
                sessions,
                tokens,
                new AuthSessionProperties(SingleSessionPolicy.REVOKE_PREVIOUS, Duration.ofMinutes(30)));
    }

    private AuthenticationService serviceFor(
            CredentialStore credentials,
            PasswordHasher passwordHasher,
            SessionRepository sessions,
            JwtTokenPort tokens,
            AuthSessionProperties sessionProperties) {
        return new AuthenticationService(credentials, passwordHasher, sessions, tokens, CLOCK, sessionProperties);
    }

    private static final class RecordingSessionRepository implements SessionRepository {

        private final Map<SessionId, Session> values = new HashMap<>();
        private Session lastOpened;
        private SingleSessionPolicy lastPolicy;

        @Override
        public Session open(Session session, SingleSessionPolicy policy) {
            lastOpened = session;
            lastPolicy = policy;
            values.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<Session> findById(SessionId sessionId) {
            return Optional.ofNullable(values.get(sessionId));
        }

        @Override
        public void revoke(SessionId sessionId) {
            var session = values.get(sessionId);
            if (session != null) {
                values.put(sessionId, session.revoke());
            }
        }
    }

    private static final class RecordingJwtTokenPort implements JwtTokenPort {

        private Session lastSession;
        private boolean activeClaim = true;

        @Override
        public String issue(String subject, Session session) {
            lastSession = session;
            return "token";
        }

        @Override
        public JwtClaims verify(String token) {
            return new JwtClaims(
                    "alice", lastSession.id(), "issuer", NOW, NOW.plus(Duration.ofMinutes(30)), activeClaim);
        }
    }

    private static final class SessionTokenPort implements JwtTokenPort {

        private final Map<String, JwtClaims> claimsByToken = new HashMap<>();
        private int sequence;

        @Override
        public String issue(String subject, Session session) {
            var token = "token-" + ++sequence;
            claimsByToken.put(token, new JwtClaims(subject, session.id(), "issuer", NOW, session.expiresAt(), true));
            return token;
        }

        @Override
        public JwtClaims verify(String token) {
            return claimsByToken.get(token);
        }
    }
}
