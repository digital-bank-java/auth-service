package com.digitalbank.authservice.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class InMemorySessionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-30T10:00:00Z");

    @Test
    void revokePreviousPolicyLeavesOnlyLatestSessionActive() {
        var repository = new InMemorySessionRepository();
        var previous = session("alice");
        var latest = session("alice");

        repository.open(previous, SingleSessionPolicy.REVOKE_PREVIOUS);
        repository.open(latest, SingleSessionPolicy.REVOKE_PREVIOUS);

        assertThat(repository.findById(previous.id()).orElseThrow().status().name())
                .isEqualTo("REVOKED");
        assertThat(repository.findById(latest.id()).orElseThrow().status().name())
                .isEqualTo("ACTIVE");
    }

    @Test
    void allowMultiplePolicyKeepsExistingSessionActive() {
        var repository = new InMemorySessionRepository();
        var previous = session("alice");
        var latest = session("alice");

        repository.open(previous, SingleSessionPolicy.ALLOW_MULTIPLE);
        repository.open(latest, SingleSessionPolicy.ALLOW_MULTIPLE);

        assertThat(repository.findById(previous.id()).orElseThrow().status().name())
                .isEqualTo("ACTIVE");
        assertThat(repository.findById(latest.id()).orElseThrow().status().name())
                .isEqualTo("ACTIVE");
    }

    @Test
    void revokeMissingSessionIsIdempotent() {
        var repository = new InMemorySessionRepository();

        repository.revoke(SessionId.newId());

        assertThat(repository.findById(SessionId.newId())).isEmpty();
    }

    private Session session(String username) {
        return Session.start(SessionId.newId(), username, NOW, NOW.plus(30, ChronoUnit.MINUTES));
    }
}
