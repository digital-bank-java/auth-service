package com.digitalbank.authservice.application.port.out;

public interface PasswordHasher {

    boolean matches(CharSequence rawPassword, String passwordHash);
}
