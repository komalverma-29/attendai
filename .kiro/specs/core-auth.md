# Specification: core-auth

## 1. Overview

`core-auth` is the authentication engine for the entire AttendAI platform. It provides stateless JWT-based authentication, token issuance, token refresh, token revocation, and password reset. It is the sole authority for verifying identity across all modules.

No business module implements its own authentication. All authentication flows route through `core-auth`.

---

## 2. Scope and Objectives

**In scope:**
- Login with email and password
- JWT access token issuance (short-lived)
- JWT refresh token issuance (long-lived)
- Access token validation on every request via Spring Security filter
- Refresh token rotation (old token invalidated on each use)
- Logout (refresh token revocation)
- Password reset via email token
- Spring Security configuration (filter chain, CORS, CSRF, public endpoints)
- `AuditorAware` integration via security context

**Out of scope:**
- User registration (belongs in `core-user`)
- Social / OAuth2 login
- Multi-factor authentication
- Session management (system is stateless)
- Role and permission loading logic (belongs in `core-role` and `core-permission`; auth loads them but does not manage them)

---

## 3. Functional Requirements

### FR-AUTH-01: Login
A client submits email and password. The system authenticates the credentials, loads the user's roles and permissions, and returns an access token and a refresh token.

### FR-AUTH-02: JWT Access Token
- The access token is a signed JWT.
- Expiry: 15 minutes.
- Algorithm: HS256 (development), RS256 (production).
- Claims: `sub` (user ID as string), `email`, `roles` (list of role codes), `permissions` (list of permission codes), `iat`, `exp`.
- The token is returned in the response body. It is not set as a cookie.

### FR-AUTH-03: JWT Refresh Token
- The refresh token is a signed JWT with a longer expiry.
- Expiry: 7 days.
- The token is stored hashed (SHA-256) in the database to support revocation.
- The raw token is returned in the response body only at issuance.
- Refresh tokens are bound to a user and a device/client identifier (optional but recommended for V1).

### FR-AUTH-04: Token Refresh
- Client submits a valid, non-expired, non-revoked refresh token.
- System validates the token, revokes it, and issues a new access token and a new refresh token (rotation).
- If the token is expired or revoked, the request fails with 401.

### FR-AUTH-05: Logout
- Client submits the refresh token.
- System marks the refresh token as revoked in the database.
- The access token cannot be revoked (stateless), so its expiry (15 minutes) is the natural TTL.
- A successful logout response is returned.

### FR-AUTH-06: Request Authentication Filter
- A `JwtAuthenticationFilter` intercepts every incoming HTTP request.
- It extracts the Bearer token from the `Authorization` header.
- It validates the token signature, expiry, and structure.
- If valid, it populates the Spring Security `SecurityContext` with a `UsernamePasswordAuthenticationToken` containing the user ID and their permission authorities.
- If invalid or missing, the filter does not throw — Spring Security's entry point returns 401.

### FR-AUTH-07: Password Reset — Request
- User submits their email address.
- If the email exists, a one-time reset token is generated, stored hashed in the database, and sent to the email via `core-notification`.
- If the email does not exist, the response is identical to the success case (no user enumeration).
- Token expiry: 1 hour.
- Only one active reset token is allowed per user. Requesting again invalidates the previous token.

### FR-AUTH-08: Password Reset — Confirm
- User submits the reset token and a new password.
- System validates the token (exists, not expired, not used).
- New password is validated against complexity rules.
- Password is hashed with BCrypt (work factor 12) and saved via `core-user`.
- Reset token is marked as used.
- All active refresh tokens for the user are revoked (force logout all sessions).
- Audit event is written via `core-audit`.

### FR-AUTH-09: Public Endpoint Declaration
The following endpoints are accessible without a JWT:
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/password-reset/request`
- `POST /api/v1/auth/password-reset/confirm`
- `GET /actuator/health`

All other endpoints require a valid JWT.

---

## 4. Non-Functional Requirements

- Token validation must add no more than 5ms of latency to a request.
- The system must support concurrent token validation without contention (stateless verification).
- Refresh token lookups must use a hashed index for O(1) lookup performance.
- The login endpoint must not reveal whether a user exists when credentials are incorrect.
- The password reset flow must not reveal whether an email is registered.

---

## 5. Business Rules

- BR-AUTH-01: A disabled or deleted user cannot authenticate. Login returns 401.
- BR-AUTH-02: Refresh tokens are single-use. Each use rotates to a new token.
- BR-AUTH-03: If a refresh token is used after revocation, all tokens for that user are revoked (token reuse detection).
- BR-AUTH-04: A user may have at most 5 concurrent active refresh tokens (5 active sessions). The oldest is revoked when the limit is exceeded.
- BR-AUTH-05: Password reset tokens expire after 1 hour and are single-use.
- BR-AUTH-06: Successful password reset invalidates all existing refresh tokens for the user.
- BR-AUTH-07: JWT signing secret must be at least 256 bits.
- BR-AUTH-08: BCrypt work factor must be exactly 12.

---

## 6. Roles and Responsibilities

`core-auth` itself does not define roles. It loads role and permission data from `core-role` and `core-permission` during authentication to populate JWT claims.

Any authenticated user can call the refresh and logout endpoints for their own tokens. No additional permission is required.

---

## 7. User Journeys

### Journey 1: Successful Login

```
Client → POST /api/v1/auth/login { email, password }
       → [core-auth] Load user by email from core-user
       → [core-auth] Verify BCrypt password
       → [core-auth] Load roles + permissions from core-role/core-permission
       → [core-auth] Generate access token (JWT)
       → [core-auth] Generate refresh token, hash + store in DB
       → [core-auth] Write audit log (LOGIN_SUCCESS) via core-audit
       → Response: { accessToken, refreshToken, expiresIn }
```

### Journey 2: Failed Login

```
Client → POST /api/v1/auth/login { email, wrongPassword }
       → [core-auth] Load user by email
       → [core-auth] BCrypt verify fails
       → [core-auth] Write audit log (LOGIN_FAILURE) via core-audit
       → Response: 401 { code: "UNAUTHORIZED", message: "Invalid credentials" }
```

### Journey 3: Token Refresh

```
Client → POST /api/v1/auth/refresh { refreshToken }
       → [core-auth] Hash incoming token, look up in DB
       → [core-auth] Validate: exists, not revoked, not expired
       → [core-auth] Revoke old token
       → [core-auth] Load fresh roles + permissions
       → [core-auth] Generate new access token + new refresh token
       → [core-auth] Store new refresh token hash
       → [core-auth] Write audit log (TOKEN_REFRESHED)
       → Response: { accessToken, refreshToken, expiresIn }
```

### Journey 4: Authenticated API Request

```
Client → GET /api/v1/... (Authorization: Bearer <accessToken>)
       → [JwtAuthenticationFilter] Extract token from header
       → [JwtAuthenticationFilter] Validate signature and expiry
       → [JwtAuthenticationFilter] Extract claims (userId, permissions)
       → [JwtAuthenticationFilter] Set SecurityContext
       → [Controller] Request proceeds normally
```

### Journey 5: Password Reset

```
Client → POST /api/v1/auth/password-reset/request { email }
       → [core-auth] Look up user (silently succeed if not found)
       → [core-auth] Generate reset token, hash + store in DB
       → [core-auth] Trigger notification via core-notification
       → Response: 200 (always, regardless of email existence)

Client → POST /api/v1/auth/password-reset/confirm { token, newPassword }
       → [core-auth] Hash incoming token, look up in DB
       → [core-auth] Validate: exists, not used, not expired
       → [core-auth] Validate password complexity
       → [core-auth] Update password hash via core-user
       → [core-auth] Mark reset token as used
       → [core-auth] Revoke all refresh tokens for user
       → [core-auth] Write audit log (PASSWORD_RESET)
       → Response: 200
```

---

## 8. Architecture and Components

### Components

| Component                  | Responsibility                                                       |
|----------------------------|----------------------------------------------------------------------|
| `AuthController`           | HTTP endpoints for login, refresh, logout, password reset            |
| `AuthService`              | Orchestrates authentication logic                                    |
| `AuthServiceImpl`          | Implements all auth workflows                                        |
| `JwtService`               | Token generation, signing, validation, claims extraction             |
| `JwtAuthenticationFilter`  | Spring Security OncePerRequestFilter for token validation            |
| `SecurityConfig`           | Spring Security filter chain, CORS, CSRF, public endpoint config     |
| `RefreshTokenRepository`   | Persists and queries hashed refresh tokens                           |
| `PasswordResetTokenRepository` | Persists and queries hashed reset tokens                         |
| `AuthMapper`               | Maps between entities and DTOs                                       |

### Dependencies on Other Core Modules

| Module           | What core-auth Uses                                            |
|------------------|----------------------------------------------------------------|
| `core-common`    | Base exceptions, response envelope, `SecurityContextUtils`     |
| `core-user`      | Load user by email, update password hash, check user status    |
| `core-role`      | Load roles assigned to a user                                  |
| `core-permission`| Load permissions for user's roles                              |
| `core-notification` | Send password reset email                                   |
| `core-audit`     | Write auth audit events                                        |

---

## 9. Entity Definitions

### RefreshToken

| Column         | Type                  | Constraints                        |
|----------------|-----------------------|------------------------------------|
| id             | BIGINT UNSIGNED       | PK, auto-increment                 |
| user_id        | BIGINT UNSIGNED       | NOT NULL, FK → users(id)           |
| token_hash     | VARCHAR(64)           | NOT NULL, UNIQUE                   |
| expires_at     | DATETIME              | NOT NULL                           |
| is_revoked     | BOOLEAN               | NOT NULL, DEFAULT FALSE            |
| revoked_at     | DATETIME              | NULL                               |
| created_at     | DATETIME              | NOT NULL, DEFAULT CURRENT_TIMESTAMP|
| created_by     | BIGINT UNSIGNED       | NULL                               |
| updated_at     | DATETIME              | NOT NULL, ON UPDATE CURRENT_TIMESTAMP|
| updated_by     | BIGINT UNSIGNED       | NULL                               |

### PasswordResetToken

| Column         | Type                  | Constraints                        |
|----------------|-----------------------|------------------------------------|
| id             | BIGINT UNSIGNED       | PK, auto-increment                 |
| user_id        | BIGINT UNSIGNED       | NOT NULL, FK → users(id)           |
| token_hash     | VARCHAR(64)           | NOT NULL, UNIQUE                   |
| expires_at     | DATETIME              | NOT NULL                           |
| is_used        | BOOLEAN               | NOT NULL, DEFAULT FALSE            |
| used_at        | DATETIME              | NULL                               |
| created_at     | DATETIME              | NOT NULL, DEFAULT CURRENT_TIMESTAMP|
| created_by     | BIGINT UNSIGNED       | NULL                               |
| updated_at     | DATETIME              | NOT NULL, ON UPDATE CURRENT_TIMESTAMP|
| updated_by     | BIGINT UNSIGNED       | NULL                               |

### Database Indexes

```sql
-- refresh_tokens
INDEX idx_refresh_tokens_user_id (user_id)
UNIQUE uq_refresh_tokens_token_hash (token_hash)
INDEX idx_refresh_tokens_user_active (user_id, is_revoked, expires_at)

-- password_reset_tokens
INDEX idx_password_reset_tokens_user_id (user_id)
UNIQUE uq_password_reset_tokens_token_hash (token_hash)
```

---

## 10. API Contracts

### POST /api/v1/auth/login

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass1"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<jwt>",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Response 401:**
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Invalid credentials"
  }
}
```

---

### POST /api/v1/auth/refresh

**Request:**
```json
{
  "refreshToken": "<jwt>"
}
```

**Response 200:** Same structure as login response.

**Response 401:** Token expired, revoked, or invalid.

---

### POST /api/v1/auth/logout

**Request:** (Authenticated)
```json
{
  "refreshToken": "<jwt>"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Logged out successfully" }
}
```

---

### POST /api/v1/auth/password-reset/request

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response 200:** Always returns success regardless of whether email exists.
```json
{
  "success": true,
  "data": { "message": "If the email exists, a reset link has been sent" }
}
```

---

### POST /api/v1/auth/password-reset/confirm

**Request:**
```json
{
  "token": "<reset-token>",
  "newPassword": "NewSecurePass1"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Password reset successfully" }
}
```

**Response 400:** Token invalid, expired, or already used. Password does not meet complexity requirements.

---

## 11. Request Validation Rules

### LoginRequest
| Field    | Rule                                      |
|----------|-------------------------------------------|
| email    | Not blank, valid email format, max 255    |
| password | Not blank, min 1, max 128 (raw input)     |

### RefreshRequest
| Field        | Rule                    |
|--------------|-------------------------|
| refreshToken | Not blank               |

### LogoutRequest
| Field        | Rule                    |
|--------------|-------------------------|
| refreshToken | Not blank               |

### PasswordResetRequest
| Field | Rule                                  |
|-------|---------------------------------------|
| email | Not blank, valid email format, max 255|

### PasswordResetConfirmRequest
| Field       | Rule                                                       |
|-------------|------------------------------------------------------------|
| token       | Not blank                                                  |
| newPassword | Not blank, min 8, max 128, at least 1 uppercase, 1 lowercase, 1 digit |

---

## 12. Security Considerations

- JWT signing secret injected via environment variable `JWT_SECRET`. Never hardcoded.
- Refresh tokens are stored as SHA-256 hashes. The raw token is never persisted.
- Password reset tokens are stored as SHA-256 hashes.
- Login endpoint does not reveal whether the email exists.
- Password reset request endpoint does not reveal whether the email is registered.
- Token reuse detection: if a revoked refresh token is submitted, all tokens for that user are revoked immediately.
- BCrypt work factor is 12, configurable via `attendai.security.bcrypt-strength` property with a minimum enforced value of 10.
- All auth events are audit-logged via `core-audit`.
- `JwtAuthenticationFilter` does not swallow exceptions silently — it delegates to `AuthenticationEntryPoint` which returns a structured 401 response.

---

## 13. Configuration

Managed via `@ConfigurationProperties(prefix = "attendai.jwt")`:

| Property                           | Default     | Description                          |
|------------------------------------|-------------|--------------------------------------|
| `attendai.jwt.secret`              | (required)  | HS256 signing secret, env var        |
| `attendai.jwt.access-token-expiry` | `900`       | Access token TTL in seconds (15 min) |
| `attendai.jwt.refresh-token-expiry`| `604800`    | Refresh token TTL in seconds (7 days)|
| `attendai.jwt.algorithm`           | `HS256`     | `HS256` or `RS256`                   |
| `attendai.security.bcrypt-strength`| `12`        | BCrypt work factor, min 10           |
| `attendai.security.max-sessions`   | `5`         | Max concurrent refresh tokens per user|
| `attendai.security.reset-token-expiry` | `3600`  | Password reset token TTL in seconds  |
| `attendai.security.allowed-origins`| (required)  | CORS allowed origins list            |

---

## 14. Error Handling

| Scenario                              | HTTP | Error Code              |
|---------------------------------------|------|-------------------------|
| Invalid credentials                   | 401  | `UNAUTHORIZED`          |
| User account disabled                 | 401  | `UNAUTHORIZED`          |
| Expired access token                  | 401  | `TOKEN_EXPIRED`         |
| Invalid token signature               | 401  | `TOKEN_INVALID`         |
| Refresh token expired                 | 401  | `REFRESH_TOKEN_EXPIRED` |
| Refresh token revoked                 | 401  | `REFRESH_TOKEN_REVOKED` |
| Password reset token expired          | 400  | `RESET_TOKEN_EXPIRED`   |
| Password reset token already used     | 400  | `RESET_TOKEN_USED`      |
| Password does not meet requirements   | 400  | `VALIDATION_FAILED`     |
| Missing Authorization header          | 401  | `UNAUTHORIZED`          |

---

## 15. Logging and Audit Requirements

All the following events must be written to `core-audit`:

| Event              | Audit Action Code   | Details                              |
|--------------------|---------------------|--------------------------------------|
| Successful login   | `AUTH_LOGIN_SUCCESS`| user_id, IP address                  |
| Failed login       | `AUTH_LOGIN_FAILURE`| attempted email (not user_id), IP    |
| Token refreshed    | `AUTH_TOKEN_REFRESH`| user_id                              |
| Logout             | `AUTH_LOGOUT`       | user_id                              |
| Password reset req | `AUTH_RESET_REQUEST`| user_id (if found)                   |
| Password reset done| `AUTH_RESET_SUCCESS`| user_id                              |
| Token reuse detect | `AUTH_TOKEN_REUSE`  | user_id, all tokens revoked          |

Service-level logging (SLF4J):
- INFO: Login success, logout, token refresh, password reset success
- WARN: Login failure, invalid token, token reuse detected
- ERROR: Unexpected exceptions during auth processing

---

## 16. Performance and Scalability

- JWT validation is CPU-only (HMAC-SHA256). No database call per request.
- Refresh token lookup uses the hashed token as the query key (UNIQUE index).
- The `JwtAuthenticationFilter` runs on every request. It must complete in under 5ms under normal load.
- Expired refresh tokens should be purged from the database by a scheduled cleanup job (frequency: daily).

---

## 17. Edge Cases and Failure Scenarios

| Scenario                                              | Handling                                       |
|-------------------------------------------------------|------------------------------------------------|
| Clock skew between server instances                   | Allow 30-second leeway in token validation     |
| User deleted after token issued                       | Filter loads user status — disabled users rejected |
| Role/permission changed after token issued            | Next refresh picks up new roles/permissions    |
| `core-notification` unavailable during reset request | Log error, return success to client (don't expose) |
| Concurrent refresh requests with same token           | Database unique constraint prevents double-use |
| JWT secret rotation                                   | Existing tokens invalidated; all users must re-login |

---

## 18. Flyway Migrations

```
V1__create_refresh_tokens_table.sql
V2__create_password_reset_tokens_table.sql
```

Location: `attendai-core/src/main/resources/db/migration/`

---

## 19. Testing Strategy

| Test Type         | Scope                                                              |
|-------------------|--------------------------------------------------------------------|
| Unit — `JwtService` | Token generation, validation, expiry, claims extraction, tampered tokens |
| Unit — `AuthServiceImpl` | Login success/failure, refresh rotation, logout, reset flows  |
| Unit — `JwtAuthenticationFilter` | Valid token, expired token, missing header, invalid signature |
| Controller test   | `AuthController` — all 5 endpoints, request validation, HTTP status codes |
| Security tests    | Public endpoints accessible without token; logout requires valid token |
| Repository test   | `RefreshTokenRepository` — find by hash, revoke, count active tokens |
| Integration test  | Full login → refresh → logout cycle with real DB                  |

Test naming: `login_shouldReturnTokens_whenValidCredentials`, `login_shouldReturn401_whenPasswordIncorrect`.

---

## 20. Implementation Roadmap

### Task 1: JWT service
- Implement `JwtService`: generate access token, generate refresh token, validate, extract claims
- Unit test all scenarios including expiry and tampered signatures

### Task 2: Security configuration
- Implement `SecurityConfig` with filter chain, CORS, CSRF disabled, public endpoint whitelist
- Implement `JwtAuthenticationFilter`
- Implement `AuthenticationEntryPoint` and `AccessDeniedHandler` returning structured responses

### Task 3: Refresh token persistence
- Flyway: `V1__create_refresh_tokens_table.sql`
- `RefreshToken` entity, `RefreshTokenRepository`
- Implement: save, find by hash, revoke, count active, revoke all for user

### Task 4: Login flow
- Implement `AuthService.login()`
- Load user, verify BCrypt, load roles/permissions, issue tokens, write audit log
- `AuthController.login()` endpoint

### Task 5: Refresh flow
- Implement `AuthService.refresh()`
- Token rotation, reuse detection, audit log
- `AuthController.refresh()` endpoint

### Task 6: Logout
- Implement `AuthService.logout()`
- `AuthController.logout()` endpoint

### Task 7: Password reset
- Flyway: `V2__create_password_reset_tokens_table.sql`
- `PasswordResetToken` entity + repository
- Implement request flow (generate, hash, store, notify)
- Implement confirm flow (validate, update password, revoke tokens)
- `AuthController.requestPasswordReset()` and `AuthController.confirmPasswordReset()`

### Task 8: Expired token cleanup
- Scheduled job to delete expired `refresh_tokens` and `password_reset_tokens`

---

## 21. Acceptance Criteria

- [ ] `POST /api/v1/auth/login` returns access and refresh tokens for valid credentials
- [ ] `POST /api/v1/auth/login` returns 401 for invalid credentials without revealing if email exists
- [ ] `POST /api/v1/auth/refresh` rotates the refresh token and issues a new access token
- [ ] A revoked refresh token cannot be used to refresh
- [ ] Using a revoked token triggers revocation of all tokens for that user
- [ ] `POST /api/v1/auth/logout` revokes the submitted refresh token
- [ ] Every authenticated request validates the JWT without a database call
- [ ] A disabled user's access token is rejected
- [ ] Password reset request always returns 200 regardless of email existence
- [ ] Password reset confirm invalidates all refresh tokens for the user
- [ ] All auth events are written to the audit log
- [ ] `JwtAuthenticationFilter` adds no more than 5ms latency per request

---

## 22. Out of Scope

- User registration (core-user)
- OAuth2 / SSO / social login
- Multi-factor authentication
- Device fingerprinting
- IP allowlisting / blocklisting
- Role and permission CRUD (core-role, core-permission)
