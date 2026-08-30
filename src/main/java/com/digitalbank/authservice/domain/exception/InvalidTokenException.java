package com.digitalbank.authservice.domain.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid authentication token");
    }

    public InvalidTokenException(Throwable cause) {
        super("Invalid authentication token", cause);
    }
}
