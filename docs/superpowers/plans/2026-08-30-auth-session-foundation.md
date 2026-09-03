# Auth Session Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bounded, internal login/logout/session foundation with signed JWTs, server-side revocation, configurable single-session policy, and explicit hexagonal ports.

**Architecture:** HTTP adapters translate validated requests into application input ports. `AuthenticationService` coordinates a credential store, password hasher, atomic session repository, and JWT port; `SessionValidationService` requires both valid JWT claims and an active domain session. Runtime adapters are BCrypt, JJWT, and synchronized in-memory stores, with production persistence explicitly deferred.

**Tech Stack:** Java 21, Spring Boot 4.0.7, Spring MVC validation, RFC 7807 `ProblemDetail`, Spring Security Crypto BCrypt, JJWT 0.12.7, JUnit 5, AssertJ, Java HTTP Client, Maven, Helm.

**Spec:** `docs/superpowers/specs/2026-08-30-auth-session-foundation-design.md`

## Global Constraints

- Never store raw passwords; credential state contains only password hashes.
- JWTs must contain `sub`, `sid`, `active`, `iss`, `iat`, and `exp` and must be signed with a runtime-injected base64 secret of at least 256 bits.
- Logout must revoke server-side session state and be idempotent for a valid bearer token.
- `REVOKE_PREVIOUS` is the default session policy; `ALLOW_MULTIPLE` is the opt-out.
- No API Gateway route, MFA, step-up, refresh rotation, external identity provider, provisioning, or production persistence is added.
- Preserve the bootstrap branch as the PR base; create and push a separate non-draft branch.

---

### Task 1: Add security and test dependencies

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Spring Security Crypto and JJWT dependencies**

Add `org.springframework.security:spring-security-crypto`, `io.jsonwebtoken:jjwt-api:0.12.7`, and runtime `jjwt-impl` and `jjwt-jackson` dependencies. Do not add the Spring Security web starter, which would change the bootstrap's HTTP security behavior.

- [ ] **Step 2: Run dependency resolution**

Run: `./mvnw --batch-mode -DskipTests dependency:resolve`

Expected: Maven resolves the new libraries without changing the application behavior.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "build: add auth token and password hashing dependencies"
```

### Task 2: Define domain session state and application/output ports

**Files:**
- Create: `src/main/java/com/digitalbank/authservice/domain/model/SessionId.java`
- Create: `src/main/java/com/digitalbank/authservice/domain/model/SessionStatus.java`
- Create: `src/main/java/com/digitalbank/authservice/domain/model/Session.java`
- Create: `src/main/java/com/digitalbank/authservice/domain/exception/AuthenticationFailedException.java`
- Create: `src/main/java/com/digitalbank/authservice/domain/exception/InvalidTokenException.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/LoginCommand.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/LoginResult.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/LoginInputPort.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/LogoutCommand.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/LogoutInputPort.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/ValidateSessionCommand.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/ValidateSessionInputPort.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/in/SessionValidationResult.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/StoredCredential.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/CredentialStore.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/PasswordHasher.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/JwtClaims.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/JwtTokenPort.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/SessionRepository.java`
- Create: `src/main/java/com/digitalbank/authservice/application/port/out/SingleSessionPolicy.java`

- [ ] **Step 1: Write domain tests for session lifecycle**

Create `src/test/java/com/digitalbank/authservice/domain/model/SessionTest.java` with tests for a newly started active session, idempotent revocation, and rejection of a non-positive expiry interval.

- [ ] **Step 2: Run the domain tests to verify the expected failure**

Run: `./mvnw -Dtest=SessionTest test`

Expected: FAIL because the domain classes do not exist yet.

- [ ] **Step 3: Implement minimal immutable domain models and ports**

Use `record SessionId(UUID value)` with `newId()`, `SessionStatus { ACTIVE, REVOKED }`, and a `Session` class exposing `start`, `revoke`, `isActiveAt`, and accessors. `Session.revoke()` must leave an already revoked instance unchanged. Keep all ports free of Spring and HTTP types.

- [ ] **Step 4: Run the domain tests**

Run: `./mvnw -Dtest=SessionTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/digitalbank/authservice/domain src/main/java/com/digitalbank/authservice/application/port src/test/java/com/digitalbank/authservice/domain
git commit -m "feat: define auth session domain ports"
```

### Task 3: Implement application authentication and session policy

**Files:**
- Create: `src/main/java/com/digitalbank/authservice/application/service/AuthenticationService.java`
- Create: `src/main/java/com/digitalbank/authservice/application/service/SessionValidationService.java`
- Create: `src/test/java/com/digitalbank/authservice/application/service/AuthenticationServiceTest.java`

**Interfaces:**
- `CredentialStore.findByUsername(String username)` returns `Optional<StoredCredential>`.
- `PasswordHasher.matches(CharSequence rawPassword, String passwordHash)` returns `boolean`.
- `SessionRepository.open(Session session, SingleSessionPolicy policy)` atomically applies the policy and returns the saved session.
- `SessionRepository.findById(SessionId sessionId)` returns `Optional<Session>`.
- `SessionRepository.revoke(SessionId sessionId)` is idempotent.
- `JwtTokenPort.issue(String subject, Session session)` returns a token string.
- `JwtTokenPort.verify(String token)` returns `JwtClaims` or throws `InvalidTokenException`.

- [ ] **Step 1: Write failing service tests**

Cover these named behaviors in `AuthenticationServiceTest`: successful login creates a session and returns a token; unknown and wrong-password users produce the same `AuthenticationFailedException`; `REVOKE_PREVIOUS` makes the previous session inactive; `ALLOW_MULTIPLE` preserves both sessions; logout revokes a session twice without failure; and session validation rejects a revoked session even when JWT claims are valid.

- [ ] **Step 2: Run the tests to verify failure**

Run: `./mvnw -Dtest=AuthenticationServiceTest test`

Expected: FAIL because the application services do not exist yet.

- [ ] **Step 3: Implement the minimal application services**

`AuthenticationService.login` must verify credentials before creating a UUID session, apply configured TTL/policy through `SessionRepository.open`, issue the JWT, and return `LoginResult`. `logout` verifies the token, finds the referenced session when present, and invokes idempotent revoke. `SessionValidationService.validate` verifies claims, finds the session, and returns success only when subject, session ID, status, and time validity all agree.

- [ ] **Step 4: Run service tests and refactor only while green**

Run: `./mvnw -Dtest=AuthenticationServiceTest test`

Expected: PASS with no password or token logging.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/digitalbank/authservice/application/service src/test/java/com/digitalbank/authservice/application/service
git commit -m "feat: implement authentication and session policy"
```

### Task 4: Add runtime configuration and adapters

**Files:**
- Create: `src/main/java/com/digitalbank/authservice/configuration/AuthConfiguration.java`
- Create: `src/main/java/com/digitalbank/authservice/configuration/AuthSessionProperties.java`
- Create: `src/main/java/com/digitalbank/authservice/configuration/AuthJwtProperties.java`
- Create: `src/main/java/com/digitalbank/authservice/configuration/FixtureIdentityProperties.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/out/security/BCryptPasswordHasher.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/out/security/JjwtTokenAdapter.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/out/identity/FixtureCredentialStore.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/out/session/InMemorySessionRepository.java`
- Create: `src/test/java/com/digitalbank/authservice/adapter/out/security/JjwtTokenAdapterTest.java`
- Create: `src/test/java/com/digitalbank/authservice/adapter/out/session/InMemorySessionRepositoryTest.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application.properties`

- [ ] **Step 1: Write failing adapter tests**

Test BCrypt matching against a precomputed hash without storing a raw password, JWT claims and signature verification, repository revocation, atomic `REVOKE_PREVIOUS`, and `ALLOW_MULTIPLE` behavior.

- [ ] **Step 2: Run adapter tests to verify failure**

Run: `./mvnw -Dtest=JjwtTokenAdapterTest,InMemorySessionRepositoryTest test`

Expected: FAIL because the adapters do not exist yet.

- [ ] **Step 3: Implement configuration and adapters**

Bind `auth.session.policy`, positive `auth.session.ttl`, nonblank `auth.jwt.issuer`, and a base64 HMAC secret of at least 32 decoded bytes. Bind fixture username/hash without a raw password field. `JjwtTokenAdapter` must use `Keys.hmacShaKeyFor`, include `sub`, `sid`, `iss`, `iat`, and `exp`, and reject bad signatures/claims. `InMemorySessionRepository.open` must synchronize the state transition and revoke active sessions before saving under `REVOKE_PREVIOUS`.

- [ ] **Step 4: Run adapter tests**

Run: `./mvnw -Dtest=JjwtTokenAdapterTest,InMemorySessionRepositoryTest test`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/com/digitalbank/authservice/configuration src/main/java/com/digitalbank/authservice/adapter src/main/resources/application.properties src/test/resources/application.properties src/test/java/com/digitalbank/authservice/adapter
git commit -m "feat: add auth runtime adapters"
```

### Task 5: Add HTTP adapters, OpenAPI, and integration tests

**Files:**
- Create: `src/main/java/com/digitalbank/authservice/adapter/in/web/AuthController.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/in/web/LoginRequest.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/in/web/LoginResponse.java`
- Create: `src/main/java/com/digitalbank/authservice/adapter/in/web/AuthApiExceptionHandler.java`
- Modify: `src/test/java/com/digitalbank/authservice/AuthServiceApplicationIT.java`
- Create: `src/test/java/com/digitalbank/authservice/AuthApiIT.java`
- Modify: `src/main/java/com/digitalbank/authservice/configuration/OpenApiConfiguration.java`

- [ ] **Step 1: Write failing HTTP integration tests**

Cover `POST /api/v1/auth/login` success with `sid` in the decoded JWT, invalid credentials returning indistinguishable `401` ProblemDetails, bean validation returning `400` with field errors, logout returning `204`, repeated logout returning `204`, and an invalid/missing bearer returning `401`. Assert OpenAPI contains both paths and documented response codes.

- [ ] **Step 2: Run the integration tests to verify failure**

Run: `./mvnw -Dit.test=AuthApiIT,AuthServiceApplicationIT failsafe:integration-test`

Expected: FAIL because the controller and exception handler do not exist yet.

- [ ] **Step 3: Implement transport-only HTTP adapters**

Use `@Valid` request records with `@NotBlank` and `@Size`; do not log request bodies or authorization headers. The controller maps bearer header syntax to `LogoutCommand`, returns `LoginResponse`, and returns `204` for logout. The advice maps authentication and token failures to stable `ProblemDetail` types and maps Spring validation exceptions to the existing field-error shape. Add OpenAPI annotations for request/response schemas and `400`/`401`/`204` responses.

- [ ] **Step 4: Run integration tests**

Run: `./mvnw -Dit.test=AuthApiIT,AuthServiceApplicationIT verify`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/digitalbank/authservice/adapter/in src/main/java/com/digitalbank/authservice/configuration/OpenApiConfiguration.java src/test/java/com/digitalbank/authservice/AuthServiceApplicationIT.java src/test/java/com/digitalbank/authservice/AuthApiIT.java
git commit -m "feat: expose internal login and logout endpoints"
```

### Task 6: Update delivery configuration and documentation

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/resources/application.properties`
- Modify: `.github/workflows/ci.yml`
- Modify: `helm/values.yaml`
- Modify: `helm/values-sit.yaml`
- Modify: `helm/templates/deployment.yaml`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Create: `docs/insomnia/auth-service.md`

- [ ] **Step 1: Update runtime defaults and CI fixture**

Set the local non-secret defaults for port, issuer, TTL, policy, and blank runtime secret. Extend the CI mock Config Server response with a base64 test secret and BCrypt fixture hash so the image starts with the new required configuration. Keep secrets synthetic and clearly marked as CI-only.

- [ ] **Step 2: Update Helm configuration**

Document the runtime secret/fixture-hash injection contract through configurable Secret references without creating a Secret or exposing a public service route. Preserve `ClusterIP`, non-root settings, probes, and the existing Config Server URL/profile behavior.

- [ ] **Step 3: Update README, AGENTS, and Insomnia request documentation**

Document endpoint payloads, error contracts, service-local verification, configuration names, in-memory limitations, production persistence follow-up, config-repo/SIT dependency, and explicit deferred MFA/step-up/refresh/identity/gateway boundaries. Add login and logout requests using environment variables and never commit a real token/password.

- [ ] **Step 4: Run documentation formatting**

Run: `./mvnw spotless:check`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml helm README.md AGENTS.md docs/insomnia src/main/resources/application.properties src/test/resources/application.properties
git commit -m "docs: document auth session operations and delivery"
```

### Task 7: Run full verification and prepare the stacked PR

**Files:**
- No additional source files; inspect all changed files and generated test reports.

- [ ] **Step 1: Run the complete Maven gate**

Run: `./mvnw --batch-mode --no-transfer-progress verify`

Expected: PASS for unit and integration tests.

- [ ] **Step 2: Run strict Helm lint and render validation**

Run: `helm lint helm --strict --values helm/values-sit.yaml` and `helm template auth-service helm --namespace digital-bank-sit --values helm/values-sit.yaml > /tmp/auth-service-rendered.yaml && test -s /tmp/auth-service-rendered.yaml`.

Expected: both commands pass and the rendered workload remains a `ClusterIP` deployment.

- [ ] **Step 3: Build and smoke-test the container**

Run the CI-equivalent `docker build -t digital-bank-java/auth-service:ci .` and start a disposable Config Server fixture with the same synthetic secret/hash properties, then verify image user `10001:10001`, `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`, and `/v3/api-docs`.

Expected: the container starts without external identity or database services and all probes return successfully.

- [ ] **Step 4: Check the diff**

Run: `git diff --check`, `git status --short`, and inspect `git diff origin/feature/45-auth-service-bootstrap...HEAD` for secrets, raw passwords, gateway changes, and accidental generated files.

Expected: no whitespace errors, no secrets/raw passwords, and only scoped auth-service changes.

- [ ] **Step 5: Push and create the non-draft PR without merging**

```bash
git push -u origin feature/45-auth-session-foundation
gh pr create --base feature/45-auth-service-bootstrap --head feature/45-auth-session-foundation --title "feat: add auth login and session foundation" --body-file /tmp/auth-session-pr.md
```

The body must be valid Markdown with actual line breaks, link `.github` issues `#45`, `#46`, `#47`, link bootstrap PR `auth-service#1`, explain that bootstrap PR #1 merges first and this PR is stacked on it, state that no waiting period is required after bootstrap/config prerequisites are available, list verification evidence, and state that the PR is intentionally non-draft and has not been merged.
