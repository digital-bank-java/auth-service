package com.digitalbank.authservice.domain.exception;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("Authentication failed");
    }
}
