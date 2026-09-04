# AGENTS.md

## Repository Purpose

`auth-service` will own authentication, session, and authorization workflows for the Digital Bank Java platform.

This repository contains the deployable authentication and session service plus a bounded internal login/logout/session foundation. Credentials remain fixture-backed until customer identity integration is delivered; sessions are PostgreSQL-backed for restart and replica durability.

## Current Scope

- Spring Boot runtime with external Config Server support.
- Actuator health and readiness/liveness endpoints.
- Service-owned OpenAPI metadata baseline.
- Container and Helm packaging for local Kubernetes SIT.
- Internal login and logout HTTP adapters with RFC 7807 errors.
- Hexagonal application/domain ports for credentials, hashing, JWTs, and sessions.
- Signed JWTs with session IDs, an active issuance-state claim, and server-side revocation.
- PostgreSQL session persistence with Flyway migrations and transactional single-session enforcement.

## Deferred Scope

- customer identity-store integration, provisioning, and account lockout/rate limiting;
- MFA and step-up authorization;
- refresh-token rotation, external identity providers, and JWT key rotation/JWKS;
- API Gateway authentication filters and public exposure;
- audit events and security monitoring.

The fixture identity adapter remains process-configured and is not a production identity store. Session state is stored in PostgreSQL; do not replace it with process-local state in production. Never add secrets, raw passwords, signing keys, or tokens to this repository.

## Runtime And Delivery

- Default service port: `8086`.
- Runtime profile: `sit` in local Kubernetes.
- Runtime configuration comes from the external `config-repo` through Config Server.
- `auth.jwt.secret` is a base64 runtime secret with at least 32 decoded bytes.
- `auth.identity.fixture.password-hash` is a BCrypt hash; raw passwords are never stored.
- `auth.session.policy` defaults conservatively to `REVOKE_PREVIOUS` and may be set to `ALLOW_MULTIPLE`.
- `spring.datasource.*` is injected by the deployment from the service-owned PostgreSQL database and the existing SIT `postgres` Secret.
- The normal SIT access path is API Gateway; direct service access is for controlled internal verification only.
- The container runs as numeric non-root user/group `10001:10001`.

Session policy is applied at session creation. Under `REVOKE_PREVIOUS`, a PostgreSQL transaction advisory lock serializes same-username opens so prior active sessions are revoked atomically before the new session is stored; validation rejects their tokens through server-side session state. `ALLOW_MULTIPLE` intentionally preserves prior active sessions.

## Commands

```bash
./mvnw test
./mvnw verify
./mvnw spring-boot:run
helm lint helm --strict --values helm/values-sit.yaml
```

Before opening a pull request, run `./mvnw verify`, Helm lint/render validation, and `git diff --check`.

## Working Rules

- Use the organization guidance in `digital-bank-java/.github/AGENTS.md`.
- Every change requires a supporting issue, dedicated branch, and pull request.
- Keep inbound adapters, application services, domain rules, and outbound adapters separated when business behavior is introduced.
- Keep authentication APIs internal until the security model and gateway policy are implemented and reviewed.
- Do not add an API Gateway route for the current auth endpoints without a separately tracked gateway/security change.
