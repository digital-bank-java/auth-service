package com.digitalbank.authservice.adapter.out.session;

import com.digitalbank.authservice.domain.model.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class SessionJpaEntity {

    @Id
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "username", nullable = false, length = 320)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SessionStatus status;

    protected SessionJpaEntity() {}

    SessionJpaEntity(UUID sessionId, String username, Instant createdAt, Instant expiresAt, SessionStatus status) {
        this.sessionId = sessionId;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    UUID sessionId() {
        return sessionId;
    }

    String username() {
        return username;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    SessionStatus status() {
        return status;
    }
}
