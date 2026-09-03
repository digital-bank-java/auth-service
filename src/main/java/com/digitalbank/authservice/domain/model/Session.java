package com.digitalbank.authservice.domain.model;

import java.time.Instant;
import java.util.Objects;

public final class Session {

    private final SessionId id;
    private final String username;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final SessionStatus status;

    private Session(SessionId id, String username, Instant createdAt, Instant expiresAt, SessionStatus status) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = requireText(username, "username");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public static Session start(SessionId id, String username, Instant createdAt, Instant expiresAt) {
        return new Session(id, username, createdAt, expiresAt, SessionStatus.ACTIVE);
    }

    public Session revoke() {
        if (status == SessionStatus.REVOKED) {
            return this;
        }
        return new Session(id, username, createdAt, expiresAt, SessionStatus.REVOKED);
    }

    public boolean isActiveAt(Instant instant) {
        Objects.requireNonNull(instant, "instant must not be null");
        return status == SessionStatus.ACTIVE && !instant.isBefore(createdAt) && instant.isBefore(expiresAt);
    }

    public SessionId id() {
        return id;
    }

    public String username() {
        return username;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public SessionStatus status() {
        return status;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
