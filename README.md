# Authentication and Session Service

Authentication and session service for the Digital Bank Java platform. The service is built with Spring Boot 4.0.7 and Java 21.

## Current Scope

This slice provides an internal, bounded authentication foundation:

- Config Client integration for externalized runtime configuration;
- Actuator health, liveness, and readiness endpoints;
- service-owned OpenAPI metadata;
- container and Helm packaging for local Kubernetes SIT;
- `POST /api/v1/auth/login` with validated credentials and signed JWT issuance;
- `POST /api/v1/auth/logout` with server-side, idempotent session revocation;
- configurable `REVOKE_PREVIOUS` or `ALLOW_MULTIPLE` session policy, defaulting conservatively to `REVOKE_PREVIOUS`;
- PostgreSQL-backed session persistence with Flyway migrations, durable token lookup, and replica-safe same-user session policy;
- Maven verification with Spotless, JaCoCo, Surefire, and Failsafe.

The fixture identity adapter is still configuration-backed and is not a customer identity store. Session state is persisted in PostgreSQL and is durable across restarts and service replicas.

## Architecture Boundary

The service follows the organization hexagonal architecture:

```text
HTTP/API Gateway -> inbound adapters -> input ports -> application/domain
                                                    -> output ports -> infrastructure adapters
```

Authentication policy remains in the application and domain layers. The controller translates transport data only. Credential lookup, password hashing, JWT operations, and session storage are outbound ports with replaceable adapters. The session validation port requires both a valid signed token and active server-side state.

## Runtime Configuration

The service reads runtime configuration from the external Config Server:

```properties
spring.application.name=auth-service
spring.config.import=configserver:${CONFIG_SERVER_URL:http://localhost:8888}

auth.session.policy=REVOKE_PREVIOUS
auth.session.ttl=PT30M
auth.jwt.issuer=digital-bank-auth
auth.jwt.secret=${AUTH_JWT_SECRET:}
auth.jwt.scopes=${AUTH_JWT_SCOPES:}
auth.identity.fixture.username=${AUTH_FIXTURE_USERNAME:}
auth.identity.fixture.password-hash=${AUTH_FIXTURE_PASSWORD_HASH:}
```

The Helm deployment supplies `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` from the service database and the existing SIT `postgres` Secret. Hibernate schema generation is disabled; Flyway owns the `auth_sessions` schema.

The formal environments are `sit`, `uat`, and `prod`. Local Kubernetes SIT uses the `sit` profile. `auth.jwt.secret` must be a base64 value decoding to at least 32 bytes and must come from Config Server or an approved secret mechanism. The fixture adapter accepts only a BCrypt password hash; it never stores a raw password. Do not commit credentials, signing keys, tokens, or customer data.

`auth.session.policy` is enforced when a session is created. With the default `REVOKE_PREVIOUS` policy, a successful login revokes all previously active sessions for the same username before the new session is stored. A token from an invalidated session is rejected by server-side session validation even when its JWT signature and `active` claim are still valid. `ALLOW_MULTIPLE` is an explicit opt-out that keeps multiple active sessions for the same username.

For Helm deployments, provision the existing `postgres` Secret before rollout. Set `secrets.enabled=true` and provision the configured Auth Secret when the JWT secret and fixture hash are delivered through Kubernetes. With the default `false`, those auth values must be supplied by the approved Config Server/secret integration.

The related Config Repo change [PR #32](https://github.com/digital-bank-java/config-repo/pull/32) supplies the non-secret Auth defaults/profile and shared SIT JWT contract. This PR does not modify the separate config repository.

## Prerequisites

- Java 21.
- Git.
- Docker Desktop with Kubernetes enabled for local SIT deployment.
- Helm 4.x for chart validation.
- Network access to Maven Central for initial dependency downloads.

The Maven Wrapper is included, so a global Maven installation is not required.

```bash
java -version
./mvnw --version
docker version
helm version --short
```

## Verify Locally

Run the complete Maven quality gate:

```bash
./mvnw verify
```

The current integration tests start the application on a random port and verify:

- `/actuator/health` returns `200` and `UP`;
- `/v3/api-docs` returns the explicit service title, internal description, contract version `1.0.0`, and both auth paths;
- login returns a JWT with signed `sub`, `sid`, `active`, `iss`, `iat`, and `exp` claims, plus the configured space-delimited `scope` claim when scopes are configured;
- logout revokes the session server-side and is idempotent;
- Flyway creates the session table, and concurrent `REVOKE_PREVIOUS` opens leave only one session active.

Validate the Helm chart:

```bash
helm lint helm --strict --values helm/values-sit.yaml
helm template auth-service helm \
  --namespace digital-bank-sit \
  --values helm/values-sit.yaml \
  | kubectl apply --dry-run=client -f -
```

## Run From An IDE

For workstation debugging against SIT, first confirm the SIT dependencies are healthy. Start the service with temporary environment variables rather than committing workstation configuration:

```text
SPRING_PROFILES_ACTIVE=sit
CONFIG_SERVER_URL=http://localhost:8888
```

Port-forward Config Server from the `digital-bank-sit` namespace before starting the process. The service listens on port `8086` when that value is supplied by Config Server or an IDE run configuration. Direct workstation access is a debugging technique; normal integrated API testing goes through the API Gateway.

For service-local fixture testing, provide temporary values such as `AUTH_JWT_SECRET`, `AUTH_FIXTURE_USERNAME`, and `AUTH_FIXTURE_PASSWORD_HASH`. Use a generated BCrypt hash and never put the source password in configuration or logs.

## Authentication API

The endpoints are service-local and intentionally have no API Gateway route in this PR.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "alice@example.com",
  "password": "<fixture-password>"
}
```

The `200 OK` response contains `accessToken`, `tokenType`, `sessionId`, and `expiresAt`. The JWT is signed and includes `sub`, `sid`, `iss`, `iat`, and `exp`, plus the configured space-delimited `scope` claim when scopes are configured; `sid` equals the response `sessionId`.

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <access-token>
```

Logout returns `204 No Content`. Repeating the request with the same valid token returns `204` again. A valid token alone is not sufficient for future protected-service authorization after its session is revoked; consumers must use the session validation application port until a later gateway/service authorization slice exists.

Errors use RFC 7807 `ProblemDetail`: validation failures are `400` with `type` ending in `validation-error`; invalid credentials are `401` with `type` ending in `authentication-failed`; malformed, missing, expired, or revoked bearer tokens are `401` with `type` ending in `invalid-token`.

## Container

Build the image with the repository naming convention:

```bash
docker build -t digital-bank-java/auth-service:0.0.2 .
```

The runtime image uses the non-root numeric user/group `10001:10001`, a read-only root filesystem-compatible layout, and exposes port `8086`.

## Kubernetes SIT

The chart uses `values-sit.yaml` for the local Kubernetes environment:

```bash
helm upgrade --install auth-service helm \
  --namespace digital-bank-sit \
  --create-namespace \
  --values helm/values-sit.yaml \
  --wait \
  --timeout 5m

kubectl rollout status deployment/auth-service \
  --namespace digital-bank-sit \
  --timeout=180s
```

The service is a `ClusterIP` workload. It is not directly exposed outside the cluster by this chart. Add API Gateway routing only in a separately tracked platform configuration change after the authentication contract and security policy exist.

## OpenAPI

The service-owned contract is available at:

```text
GET http://localhost:8086/v3/api-docs
```

The centralized internal documentation path will be added to API Gateway only when a reviewed service contract and access policy are available.

Insomnia request definitions are documented in [`docs/insomnia/auth-service.md`](docs/insomnia/auth-service.md).

## Explicit Boundaries

MFA, step-up authorization, refresh-token rotation, external identity providers, customer provisioning, account lockout/rate limiting, audit events, JWT key rotation/JWKS, and API Gateway authentication filters are deferred follow-up slices. The fixture identity adapter is not a substitute for customer identity integration.

## Development Workflow

Use a dedicated branch and pull request for every change. Before opening a pull request:

```bash
git status
./mvnw verify
helm lint helm --strict --values helm/values-sit.yaml
git diff --check
```

Relevant organization stories: [`.github#45`](https://github.com/digital-bank-java/.github/issues/45), [`.github#46`](https://github.com/digital-bank-java/.github/issues/46), and [`.github#47`](https://github.com/digital-bank-java/.github/issues/47). The implementation is stacked on [auth-service bootstrap PR #1](https://github.com/digital-bank-java/auth-service/pull/1).

## Operational Logging

The service emits one-line ECS JSON console events and propagates the bounded
`X-Correlation-ID` boundary defined in the organization [structured logging and redaction contract](https://github.com/digital-bank-java/.github/blob/main/docs/structured-logging-and-redaction.md). Passwords, credentials, tokens, session identifiers, and identity data are not logged.
