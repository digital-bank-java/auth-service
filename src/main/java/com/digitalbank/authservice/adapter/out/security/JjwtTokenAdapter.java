package com.digitalbank.authservice.adapter.out.security;

import com.digitalbank.authservice.application.port.out.JwtClaims;
import com.digitalbank.authservice.application.port.out.JwtTokenPort;
import com.digitalbank.authservice.configuration.AuthJwtProperties;
import com.digitalbank.authservice.domain.exception.InvalidTokenException;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import com.digitalbank.authservice.domain.model.SessionStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

public class JjwtTokenAdapter implements JwtTokenPort {

    private final AuthJwtProperties properties;
    private final SecretKey signingKey;

    public JjwtTokenAdapter(AuthJwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.secret()));
    }

    @Override
    public String issue(String subject, Session session) {
        if (session.status() != SessionStatus.ACTIVE) {
            throw new IllegalArgumentException("session must be active");
        }
        var builder = Jwts.builder()
                .subject(subject)
                .claim("sid", session.id().value().toString())
                .claim("active", true)
                .claim("aud", properties.audiences())
                .claim("token_purpose", properties.tokenPurpose())
                .issuer(properties.issuer())
                .issuedAt(Date.from(session.createdAt()))
                .expiration(Date.from(session.expiresAt()));
        if (!properties.scopes().isEmpty()) {
            builder.claim("scope", String.join(" ", properties.scopes()));
        }
        return builder.signWith(signingKey).compact();
    }

    @Override
    public JwtClaims verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            var subject = claims.getSubject();
            var sessionId = claims.get("sid", String.class);
            var issuedAt = claims.getIssuedAt();
            var expiresAt = claims.getExpiration();
            var active = claims.get("active", Boolean.class);
            var scopes = parseScopes(claims.get("scope", String.class));
            var audiences = claims.getAudience();
            var tokenPurpose = claims.get("token_purpose", String.class);
            if (subject == null
                    || subject.isBlank()
                    || sessionId == null
                    || issuedAt == null
                    || expiresAt == null
                    || active == null
                    || audiences == null
                    || audiences.stream().noneMatch(properties.audiences()::contains)
                    || !properties.tokenPurpose().equals(tokenPurpose)) {
                throw new InvalidTokenException();
            }
            return new JwtClaims(
                    subject,
                    new SessionId(java.util.UUID.fromString(sessionId)),
                    claims.getIssuer(),
                    issuedAt.toInstant(),
                    expiresAt.toInstant(),
                    active,
                    scopes);
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException(exception);
        }
    }

    private static List<String> parseScopes(String scopeClaim) {
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return List.of();
        }
        return List.of(scopeClaim.trim().split("\\s+"));
    }
}
