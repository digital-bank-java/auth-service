package com.digitalbank.authservice.application.port.out;

import java.util.Optional;

public interface CredentialStore {

    Optional<StoredCredential> findByUsername(String username);
}
