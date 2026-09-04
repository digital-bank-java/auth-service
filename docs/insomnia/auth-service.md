# Auth Service Insomnia Requests

These requests target the service directly for controlled internal verification. No API Gateway route is included in this slice.

## Environment

```text
auth_base_url = http://localhost:8086
auth_username = alice@example.com
auth_password = <fixture-password>
auth_access_token = <set from the login response>
```

Use a temporary environment and do not commit a real password or token.

## Login

```http
POST {{ auth_base_url }}/api/v1/auth/login
Content-Type: application/json

{
  "username": "{{ auth_username }}",
  "password": "{{ auth_password }}"
}
```

Expected response: `200 OK` with `accessToken`, `tokenType`, `sessionId`, and `expiresAt`. Copy the returned access token to `auth_access_token` for the logout request. The decoded JWT contains signed `sub`, `sid`, `active`, `iss`, `iat`, and `exp` claims, plus the configured space-delimited `scope` claim when scopes are configured; `active` is `true` when issued and `sid` matches `sessionId`. Current authorization still requires the server-side session to remain active.

## Logout

```http
POST {{ auth_base_url }}/api/v1/auth/logout
Authorization: Bearer {{ auth_access_token }}
```

Expected response: `204 No Content`. Repeating this request verifies idempotent logout. Invalid credentials return RFC 7807 `401` responses; invalid login payloads return RFC 7807 `400` responses with field errors.

## Setup Boundary

The fixture identity adapter requires a runtime username and BCrypt hash through the approved Config Server/Secret mechanism; it never stores the source password. Session state is persisted in PostgreSQL through Flyway and remains available across service restarts and replicas. Provision the existing `postgres` Secret before SIT rollout.
