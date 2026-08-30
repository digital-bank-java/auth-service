# AGENTS.md

## Repository Purpose

`auth-service` will own authentication, session, and authorization workflows for the Digital Bank Java platform.

This repository currently contains only the production delivery scaffold. Login, logout, token issuance, session persistence, MFA, and authorization rules are future slices.

## Current Scope

- Spring Boot runtime with external Config Server support.
- Actuator health and readiness/liveness endpoints.
- Service-owned OpenAPI metadata baseline.
- Container and Helm packaging for local Kubernetes SIT.

## Future Scope

- credential verification and password policy;
- token or session lifecycle;
- single-session policy;
- MFA and step-up authorization;
- audit events and security monitoring.

Do not add secrets, passwords, signing keys, or tokens to this repository.

## Runtime And Delivery

- Default service port: `8086`.
- Runtime profile: `sit` in local Kubernetes.
- Runtime configuration comes from the external `config-repo` through Config Server.
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
