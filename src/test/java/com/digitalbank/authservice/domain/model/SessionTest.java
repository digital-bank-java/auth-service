package com.digitalbank.authservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-30T10:00:00Z");
    private static final Instant EXPIRES_AT = CREATED_AT.plus(30, ChronoUnit.MINUTES);

    @Test
    void startsActiveUntilExpiry() {
        var session = Session.start(new SessionId(UUID.randomUUID()), "alice@example.com", CREATED_AT, EXPIRES_AT);

        assertThat(session.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.isActiveAt(CREATED_AT.plusSeconds(1))).isTrue();
        assertThat(session.isActiveAt(EXPIRES_AT)).isFalse();
    }

    @Test
    void revocationIsIdempotent() {
        var session = Session.start(new SessionId(UUID.randomUUID()), "alice@example.com", CREATED_AT, EXPIRES_AT);

        var revoked = session.revoke();

        assertThat(revoked.status()).isEqualTo(SessionStatus.REVOKED);
        assertThat(revoked.revoke()).isSameAs(revoked);
        assertThat(revoked.isActiveAt(CREATED_AT.plusSeconds(1))).isFalse();
    }

    @Test
    void rejectsExpiryAtOrBeforeCreation() {
        assertThatIllegalArgumentException()
                .isThrownBy(() ->
                        Session.start(new SessionId(UUID.randomUUID()), "alice@example.com", CREATED_AT, CREATED_AT));
    }
}
