# Auth Session Foundation Design

**Date:** 2026-08-30

**Issues:** [digital-bank-java/.github#45](https://github.com/digital-bank-java/.github/issues/45), [#46](https://github.com/digital-bank-java/.github/issues/46), [#47](https://github.com/digital-bank-java/.github/issues/47)

## Goal

Deliver a bounded login/logout/session foundation in `auth-service` with explicit hexagonal ports, validated HTTP models, server-side session revocation, configurable single-active-session behavior, and signed JWTs containing a session identifier.

## Architecture

The application service owns authentication and session policy. Inbound HTTP adapters validate and translate requests; outbound ports abstract credential lookup, password hashing, JWT operations, and session state.

This repository has no identity database or persistence foundation. The first adapter is an in-memory credential/session implementation that preserves active and revoked state for the lifetime of the process. The ports and domain model define the replacement boundary for the next production persistence slice; this PR does not claim restart or multi-replica durability.

```text
HTTP login/logout
        -> inbound ports -> AuthenticationService / SessionValidationService
                              -> CredentialStore
                              -> PasswordHasher
                              -> JwtTokenPort
                              -> SessionRepository
```

## HTTP Contract

`POST /api/v1/auth/login` accepts a username and password. The username is non-blank and at most 254 characters; the password is non-blank and between 12 and 128 characters. A successful response is `200 OK` with `accessToken`, `tokenType` (`Bearer`), `sessionId`, and `expiresAt`.

`POST /api/v1/auth/logout` accepts a signed bearer token in the `Authorization` header. The service extracts the signed token's `sid`, revokes that session, and returns `204 No Content`. Repeating logout with the same valid token is also `204`; the operation is idempotent. Missing or invalid bearer credentials return `401`.

No API Gateway route is added. The service remains internal until gateway authentication and access policy are reviewed.

Errors use RFC 7807 `ProblemDetail` with stable problem types:

- `https://digital-bank-java.local/problems/validation-error` (`400`)
- `https://digital-bank-java.local/problems/authentication-failed` (`401`)
- `https://digital-bank-java.local/problems/invalid-token` (`401`)

Invalid credentials use the same response for unknown users and wrong passwords. Passwords and tokens are never logged or returned in errors.

## Domain And Policies

Each session has a UUID `SessionId`, username, creation time, expiry time, and `ACTIVE` or `REVOKED` status. Revocation is a server-side state transition and is checked independently of JWT expiry by the session validation application port.

The configurable `auth.session.policy` defaults to `REVOKE_PREVIOUS`. Under that policy, opening a session atomically revokes the user's currently active sessions before saving the new session. `ALLOW_MULTIPLE` preserves existing active sessions. The session TTL is configurable through `auth.session.ttl` and must be positive.

The fixture identity adapter stores only a configured password hash. Password matching is behind a `PasswordHasher` port and uses BCrypt in the runtime adapter. A real customer/identity provider and user provisioning remain outside this slice.

## JWT Contract

The runtime JWT adapter signs HMAC tokens with a base64-encoded secret supplied by runtime configuration. Tokens contain `sub`, `sid`, `iss`, `iat`, and `exp` claims. The secret must be at least 256 bits and is never committed.

JWT issuance and verification are outbound ports. Session validation requires both a valid signed token and an active, unexpired session record, so token expiry is not treated as logout.

## Configuration And Delivery

Local application defaults provide non-secret policy values. The signing secret and fixture password hash are injected through Config Server or the deployment secret mechanism. The current Config Server repository has no `auth-service` service files, so the README documents the required follow-up config-repo slice for SIT, UAT, and production values.

Helm remains `ClusterIP` and receives no public route. CI's disposable Config Server fixture supplies non-production test values so the container smoke test can exercise the new startup contract.

## Explicitly Deferred

- Postgres/Redis-backed session persistence and multi-replica consistency.
- Customer identity-store integration and full user provisioning.
- MFA, step-up authorization, recovery codes, and account lockout/rate limiting.
- Refresh-token rotation, token exchange, external identity providers, and key rotation/JWKS.
- API Gateway authentication filters and public exposure.
- Authentication audit events and security monitoring.

These are follow-up issues/slices, not hidden implementation assumptions in this PR.

## Verification

Unit tests cover credential failure, session policy, revocation, JWT claims, validation, and idempotent logout. Spring integration tests cover the HTTP login/logout contract and OpenAPI metadata. The delivery gate remains `./mvnw verify`, strict Helm lint/template rendering, Docker health/OpenAPI smoke, and `git diff --check`.
