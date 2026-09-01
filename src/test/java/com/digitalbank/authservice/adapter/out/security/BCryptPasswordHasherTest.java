package com.digitalbank.authservice.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private static final String PASSWORD_HASH = "$2y$10$PfhL7Cgz2pObXHdObp8BduGM4qggrEBtd6Oilnx0boKry0KfIVvnG";

    @Test
    void matchesPasswordAgainstStoredHash() {
        var hasher = new BCryptPasswordHasher();

        assertThat(hasher.matches("password", PASSWORD_HASH)).isTrue();
        assertThat(hasher.matches("wrong-password", PASSWORD_HASH)).isFalse();
    }
}
