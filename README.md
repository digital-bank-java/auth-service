# Authentication and Session Service

Authentication and session service scaffold for the Digital Bank Java platform. The service is built with Spring Boot 4.0.7 and Java 21 and is prepared for the future authentication delivery slices.

## Current Scope

This slice establishes a deployable service boundary only:

- Config Client integration for externalized runtime configuration;
- Actuator health, liveness, and readiness endpoints;
- service-owned OpenAPI metadata;
- container and Helm packaging for local Kubernetes SIT;
- Maven verification with Spotless, JaCoCo, Surefire, and Failsafe.

Login, logout, credential verification, token issuance, session persistence, MFA, step-up authorization, Kafka events, and database persistence are intentionally future work. No authentication business endpoint is exposed by this scaffold.

## Architecture Boundary

When business behavior is introduced, the service will follow the organization hexagonal architecture:

```text
HTTP/API Gateway -> inbound adapters -> input ports -> application/domain
                                                    -> output ports -> infrastructure adapters
```

Authentication policy remains in the application and domain layers. Controllers and future messaging adapters will translate transport data only. Persistence, token providers, and external identity integrations will remain outbound adapters.

## Runtime Configuration

The service reads runtime configuration from the external Config Server:

```properties
spring.application.name=auth-service
spring.config.import=configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

The formal environments are `sit`, `uat`, and `prod`. Local Kubernetes SIT uses the `sit` profile. Real credentials, signing keys, tokens, and passwords must be provided by the deployment secret mechanism and must never be committed here.

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
- `/v3/api-docs` returns the explicit service title, internal description, and contract version `1.0.0`.

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

## Container

Build the image with the repository naming convention:

```bash
docker build -t digital-bank-java/auth-service:0.0.1 .
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

## Development Workflow

Use a dedicated branch and pull request for every change. Before opening a pull request:

```bash
git status
./mvnw verify
helm lint helm --strict --values helm/values-sit.yaml
git diff --check
```

Relevant organization story: [`.github#45`](https://github.com/digital-bank-java/.github/issues/45).
