package com.digitalbank.authservice.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID value) {

    public SessionId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static SessionId newId() {
        return new SessionId(UUID.randomUUID());
    }
}
