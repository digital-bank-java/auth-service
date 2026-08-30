package com.digitalbank.authservice.adapter.in.web;

import com.digitalbank.authservice.domain.exception.AuthenticationFailedException;
import com.digitalbank.authservice.domain.exception.InvalidTokenException;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class AuthApiExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://digital-bank-java.local/problems/validation-error");
    private static final URI AUTHENTICATION_TYPE =
            URI.create("https://digital-bank-java.local/problems/authentication-failed");
    private static final URI INVALID_TOKEN_TYPE = URI.create("https://digital-bank-java.local/problems/invalid-token");

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> handleAuthenticationFailure(AuthenticationFailedException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Authentication failed");
        problem.setType(AUTHENTICATION_TYPE);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ProblemDetail> handleInvalidToken(InvalidTokenException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("Invalid token");
        problem.setType(INVALID_TOKEN_TYPE);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidationFailure(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Invalid request");
        problem.setType(VALIDATION_TYPE);
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
