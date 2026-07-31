# AttendAI — Security Philosophy

## Guiding Principle

Security is not a feature added at the end. It is built into every layer from the start. Secure defaults are always applied. Permissive exceptions require explicit justification.

---

## Authentication

- Authentication is handled entirely by `attendai-core` (`core-auth`).
- No business module implements its own authentication.
- All API endpoints are secured by default. Public endpoints must be explicitly declared.
- Authentication uses stateless JWT. No server-side session state.

### Authentication Flow

1. Client submits credentials to `POST /api/v1/auth/login`.
2. Core validates credentials against the user store.
3. On success, Core issues an **access token** (short-lived) and a **refresh token** (long-lived).
4. Client includes the access token in the `Authorization: Bearer <token>` header on every request.
5. Spring Security validates the token on each request via a `JwtAuthenticationFilter`.
6. On access token expiry, client uses `POST /api/v1/auth/refresh` with the refresh token.
7. Refresh tokens are rotated on each use.

---

## JWT Standards

- Algorithm: **HS256** (minimum) or **RS256** (preferred for production).
- Access token expiry: **15 minutes**.
- Refresh token expiry: **7 days**.
- Token payload (claims) must include:
  - `sub` — user ID
  - `email` — user email
  - `roles` — list of role codes
  - `permissions` — list of permission codes
  - `iat` — issued at
  - `exp` — expiration
- Token payload must never include passwords, raw IDs of sensitive entities, or PII beyond what is listed above.
- The JWT signing secret must be stored in environment variables, not in code or `application.yml`.
- Refresh tokens are stored (hashed) in the database to support revocation.

---

## Authorization

- Authorization uses **permission-based access control**, not role-based checks in code.
- Roles are groupings of permissions. Authorization decisions are made against permissions.
- Method-level security using `@PreAuthorize("hasAuthority('PERMISSION_CODE')")`.
- Permission codes follow the pattern: `<MODULE>_<RESOURCE>_<ACTION>`.

Examples:
```
SCHOOL_STUDENT_CREATE
SCHOOL_STUDENT_READ
SCHOOL_STUDENT_UPDATE
SCHOOL_STUDENT_DELETE
SCHOOL_ATTENDANCE_MARK
CORE_USER_MANAGE
```

- `@EnableMethodSecurity` must be enabled in the security configuration.
- Administrators of business modules can only operate within their own module's scope.
- Super-admin access is restricted to system-level operations only.

---

## Password Handling

- Passwords are **never stored in plain text**.
- Password hashing algorithm: **BCrypt** with a work factor of at least 12.
- Spring Security's `BCryptPasswordEncoder` is the only permitted password encoder.
- Password reset uses time-limited, single-use tokens sent via a secure channel (email).
- Password reset tokens are stored hashed in the database.
- Password reset token expiry: **1 hour**.
- Minimum password requirements: 8 characters, at least one uppercase, one lowercase, one digit.
- Password validation is enforced at the service layer, not only at the DTO level.

---

## Input Validation

- All inbound data is validated via Jakarta Bean Validation (`@Valid` on controller parameters).
- Validation annotations are placed on Request DTOs.
- Never trust client-provided IDs for authorization decisions. Re-fetch from the database.
- String inputs must be length-constrained to prevent oversized payloads.
- File uploads must validate content type and file size limits.
- SQL injection is prevented by exclusive use of JPA/JPQL (parameterized queries). Raw string concatenation in queries is forbidden.

---

## Secure Defaults

- All endpoints require authentication unless explicitly exempted.
- Publicly accessible endpoints are limited to:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - Health check actuator endpoint (`/actuator/health`)
- CORS is configured explicitly. Wildcard `*` origins are not permitted in production.
- HTTP headers: Spring Security's default security headers are enabled (HSTS, X-Content-Type-Options, X-Frame-Options, etc.).
- CSRF protection is disabled for stateless JWT APIs (standard practice for REST APIs).
- Sensitive actuator endpoints (`/actuator/env`, `/actuator/beans`, etc.) are disabled or protected in production.

---

## Secrets and Configuration

- Secrets (JWT signing key, database password, API keys) are never stored in code or committed to version control.
- Secrets are injected via environment variables at runtime.
- `application.yml` references secrets via `${ENV_VAR_NAME}` placeholders.
- `.env` files are for local development only and are listed in `.gitignore`.

---

## Audit Logging

- All authentication events are audit-logged: login success, login failure, logout, token refresh, password reset.
- All authorization failures (403) are audit-logged.
- All write operations (create, update, delete) on domain entities are audit-logged.
- Audit logs include: timestamp, user ID, action, affected resource type, affected resource ID, IP address.
- Audit logs are immutable. They are append-only and must never be updated or soft-deleted.
- Audit logs are written by the `core-audit` module via a service call or AOP interceptor.

---

## Exception Handling and Security

- Error responses must never leak stack traces, internal class names, query details, or infrastructure information.
- Authentication failures return `401 Unauthorized` with a generic message ("Invalid credentials").
- Authorization failures return `403 Forbidden` with a generic message.
- Do not distinguish between "user not found" and "wrong password" in login error messages (prevents user enumeration).
- Validation error responses reveal field names and validation messages but not internal implementation details.

---

## Transport Security

- All production traffic must use HTTPS (TLS 1.2 minimum, TLS 1.3 preferred).
- HTTP is not accepted in production. Redirect or reject HTTP connections.
- SSL termination may occur at the load balancer or reverse proxy level.

---

## Dependency Security

- Dependencies are pinned to explicit versions (no open ranges).
- New dependencies must be reviewed before addition.
- The Maven dependency tree should be reviewed periodically for known vulnerabilities.
- Use OWASP Dependency-Check or similar tooling as part of the CI pipeline.
