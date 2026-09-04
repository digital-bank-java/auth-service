package com.digitalbank.authservice.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
class AuthPersistenceIT {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.config.enabled", () -> "false");
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }

    @Autowired
    private PostgresSessionRepository repository;

    @Test
    void persistsFindsAndRevokesSession() {
        var session = session("alice-" + UUID.randomUUID());

        repository.open(session, SingleSessionPolicy.ALLOW_MULTIPLE);

        assertThat(repository.findById(session.id())).hasValueSatisfying(saved -> {
            assertThat(saved.id()).isEqualTo(session.id());
            assertThat(saved.username()).isEqualTo(session.username());
            assertThat(saved.createdAt()).isEqualTo(session.createdAt());
            assertThat(saved.expiresAt()).isEqualTo(session.expiresAt());
            assertThat(saved.status().name()).isEqualTo("ACTIVE");
        });

        repository.revoke(session.id());

        assertThat(repository.findById(session.id()).orElseThrow().status().name())
                .isEqualTo("REVOKED");
    }

    @Test
    void concurrentRevokePreviousOpensLeaveOnlyOneActiveSession() throws Exception {
        var username = "concurrent-" + UUID.randomUUID();
        var sessions = List.of(session(username), session(username));
        var ready = new CountDownLatch(sessions.size());
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(sessions.size())) {
            var futures = sessions.stream()
                    .map(session -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        repository.open(session, SingleSessionPolicy.REVOKE_PREVIOUS);
                        return session;
                    }))
                    .toList();

            ready.await();
            start.countDown();
            futures.forEach(future -> assertThat(future).succeedsWithin(10, TimeUnit.SECONDS));
        }

        assertThat(sessions.stream()
                        .map(session -> repository
                                .findById(session.id())
                                .orElseThrow()
                                .status()
                                .name())
                        .toList())
                .containsExactlyInAnyOrder("REVOKED", "ACTIVE");
    }

    private static Session session(String username) {
        var createdAt = Instant.parse("2026-09-04T02:00:00Z");
        return Session.start(SessionId.newId(), username, createdAt, createdAt.plus(30, ChronoUnit.MINUTES));
    }
}
