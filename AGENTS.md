# AGENTS.md

## Repository Purpose

`auth-service` will own authentication, session, and authorization workflows for the Digital Bank Java platform.

This repository contains the deployable scaffold plus a bounded internal login/logout/session foundation. Production identity and session persistence remain future slices.

## Current Scope

- Spring Boot runtime with external Config Server support.
- Actuator health and readiness/liveness endpoints.
- Service-owned OpenAPI metadata baseline.
- Container and Helm packaging for local Kubernetes SIT.
- Internal login and logout HTTP adapters with RFC 7807 errors.
- Hexagonal application/domain ports for credentials, hashing, JWTs, and sessions.
- Signed JWTs with session IDs, an active issuance-state claim, and server-side revocation.

## Deferred Scope

- Postgres/Redis session persistence and multi-replica consistency;
- customer identity-store integration, provisioning, and account lockout/rate limiting;
- MFA and step-up authorization;
- refresh-token rotation, external identity providers, and JWT key rotation/JWKS;
- API Gateway authentication filters and public exposure;
- audit events and security monitoring.

The current fixture identity and session adapters are process-lifetime in-memory implementations. Do not treat them as production persistence. Never add secrets, raw passwords, signing keys, or tokens to this repository.

## Runtime And Delivery

- Default service port: `8086`.
- Runtime profile: `sit` in local Kubernetes.
- Runtime configuration comes from the external `config-repo` through Config Server.
- `auth.jwt.secret` is a base64 runtime secret with at least 32 decoded bytes.
- `auth.identity.fixture.password-hash` is a BCrypt hash; raw passwords are never stored.
- `auth.session.policy` is `REVOKE_PREVIOUS` by default and may be set to `ALLOW_MULTIPLE`.
- The normal SIT access path is API Gateway; direct service access is for controlled internal verification only.
- The container runs as numeric non-root user/group `10001:10001`.

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
