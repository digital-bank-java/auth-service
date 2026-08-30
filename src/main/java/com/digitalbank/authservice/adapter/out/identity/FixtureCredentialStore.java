package com.digitalbank.authservice.adapter.out.identity;

import com.digitalbank.authservice.application.port.out.CredentialStore;
import com.digitalbank.authservice.application.port.out.StoredCredential;
import com.digitalbank.authservice.configuration.FixtureIdentityProperties;
import java.util.Optional;

public class FixtureCredentialStore implements CredentialStore {

    private final StoredCredential credential;

    public FixtureCredentialStore(FixtureIdentityProperties properties) {
        if (properties.username() == null
                || properties.username().isBlank()
                || properties.passwordHash() == null
                || properties.passwordHash().isBlank()) {
            credential = null;
        } else {
            credential = new StoredCredential(properties.username(), properties.passwordHash());
        }
    }

    @Override
    public Optional<StoredCredential> findByUsername(String username) {
        if (credential == null || !credential.username().equals(username)) {
            return Optional.empty();
        }
        return Optional.of(credential);
    }
}
