package com.digitalbank.authservice.application.port.out;

public record StoredCredential(String username, String passwordHash) {

    public StoredCredential {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }
}
