package com.digitalbank.authservice.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.digitalbank.authservice.application.port.out.JwtClaims;
import com.digitalbank.authservice.configuration.AuthJwtProperties;
import com.digitalbank.authservice.domain.exception.InvalidTokenException;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JjwtTokenAdapterTest {

    private static final Instant CREATED_AT = Instant.now().minus(1, ChronoUnit.MINUTES);
    private static final String SECRET =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void issuedTokenContainsSignedSessionClaims() {
        var adapter = new JjwtTokenAdapter(new AuthJwtProperties(SECRET, "digital-bank-auth"));
        var session = Session.start(
                SessionId.newId(), "alice@example.com", CREATED_AT, CREATED_AT.plus(30, ChronoUnit.MINUTES));

        var claims = adapter.verify(adapter.issue(session.username(), session));

        assertThat(claims)
                .isEqualTo(new JwtClaims(
                        session.username(),
                        session.id(),
                        "digital-bank-auth",
                        CREATED_AT.truncatedTo(ChronoUnit.SECONDS),
                        session.expiresAt().truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        var adapter = new JjwtTokenAdapter(new AuthJwtProperties(SECRET, "digital-bank-auth"));
        var otherAdapter = new JjwtTokenAdapter(new AuthJwtProperties(
                Base64.getEncoder().encodeToString("abcdef0123456789abcdef0123456789".getBytes()),
                "digital-bank-auth"));
        var session = Session.start(
                SessionId.newId(), "alice@example.com", CREATED_AT, CREATED_AT.plus(30, ChronoUnit.MINUTES));

        var token = otherAdapter.issue(session.username(), session);

        assertThatThrownBy(() -> adapter.verify(token)).isInstanceOf(InvalidTokenException.class);
    }
}
