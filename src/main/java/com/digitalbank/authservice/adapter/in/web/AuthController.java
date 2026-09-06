package com.digitalbank.authservice.adapter.in.web;

import com.digitalbank.authservice.application.port.in.LoginCommand;
import com.digitalbank.authservice.application.port.in.LoginInputPort;
import com.digitalbank.authservice.application.port.in.LogoutCommand;
import com.digitalbank.authservice.application.port.in.LogoutInputPort;
import com.digitalbank.authservice.application.port.in.ValidateSessionCommand;
import com.digitalbank.authservice.application.port.in.ValidateSessionInputPort;
import com.digitalbank.authservice.domain.exception.InvalidTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication")
class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final LoginInputPort loginInputPort;
    private final LogoutInputPort logoutInputPort;
    private final ValidateSessionInputPort validateSessionInputPort;

    AuthController(
            LoginInputPort loginInputPort,
            LogoutInputPort logoutInputPort,
            ValidateSessionInputPort validateSessionInputPort) {
        this.loginInputPort = loginInputPort;
        this.logoutInputPort = logoutInputPort;
        this.validateSessionInputPort = validateSessionInputPort;
    }

    @PostMapping(
            value = "/api/v1/auth/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an authentication session")
    @ApiResponse(responseCode = "200", description = "Authentication session created")
    @ApiResponse(
            responseCode = "400",
            description = "Request validation failed",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = loginInputPort.login(new LoginCommand(request.username(), request.password()));
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping(value = "/api/v1/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Revoke the current authentication session")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Session revoked")
    @ApiResponse(
            responseCode = "401",
            description = "Bearer token is missing or invalid",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String authorization) {
        logoutInputPort.logout(new LogoutCommand(bearerToken(authorization)));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping(value = "/api/v1/auth/session", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate the current authentication session")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Authentication session is active")
    @ApiResponse(
            responseCode = "401",
            description = "Bearer token is missing, invalid, or revoked",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<SessionValidationResponse> validateSession(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        var result = validateSessionInputPort.validate(new ValidateSessionCommand(bearerToken(authorization)));
        return ResponseEntity.ok(SessionValidationResponse.from(result));
    }

    private String bearerToken(String authorization) {
        if (authorization == null
                || !authorization.startsWith(BEARER_PREFIX)
                || authorization.substring(BEARER_PREFIX.length()).isBlank()) {
            throw new InvalidTokenException();
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
