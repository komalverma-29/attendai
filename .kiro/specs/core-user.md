# Specification: core-user

## 1. Overview

`core-user` manages the system user accounts that enable access to the AttendAI platform. A user is the authenticated identity — the account with credentials and status. A user is distinct from a person (managed by `core-person`). A person holds identity and contact information; a user holds login credentials and platform access state.

A user must always be linked to a person. A person may exist without a user (e.g., a person registered for face recognition before being granted platform access).

`core-user` is responsible for user creation, password management, status lifecycle, and querying. It exposes a service API consumed by `core-auth` for authentication and by business modules for user provisioning.

---

## 2. Scope and Objectives

**In scope:**
- User account creation (by administrators)
- User account status management (ACTIVE, INACTIVE, SUSPENDED, LOCKED)
- Password storage (BCrypt hashed) and update
- Linking a user to a `Person` record
- User lookup by ID, email, and username
- User listing with pagination and filtering
- User soft delete
- Mandatory password change on first login flag
- Role assignment to users (delegates to `core-role`)

**Out of scope:**
- Self-registration (no public registration endpoint in V1)
- User profile display (person details live in `core-person`)
- Authentication logic (belongs in `core-auth`)
- Role definitions (belongs in `core-role`)
- Permission definitions (belongs in `core-permission`)

---

## 3. Functional Requirements

### FR-USER-01: Create User
An administrator creates a user by providing an email, a username, a temporary password, and a link to an existing `Person` record. The system stores the hashed password and sets the user status to `ACTIVE` with `mustChangePassword = true`.

### FR-USER-02: Find User by ID
Retrieve a single user by their surrogate ID. Soft-deleted users are not returned.

### FR-USER-03: Find User by Email
Retrieve a single user by their email address. Used by `core-auth` during login. Must be efficient (indexed lookup).

### FR-USER-04: Find User by Username
Retrieve a single user by their username.

### FR-USER-05: List Users
Return a paginated list of users with optional filters: status, search by name or email. Soft-deleted users are excluded.

### FR-USER-06: Update User
Update permitted user fields: email, username, `mustChangePassword` flag. Password is not updated via this endpoint.

### FR-USER-07: Change Password
Update a user's password. Accepts the current password (for self-service) or is called directly by `core-auth` during password reset (bypasses current password). The new password is validated and hashed. BCrypt work factor 12.

### FR-USER-08: Activate User
Set user status to `ACTIVE`. Permitted for users in `INACTIVE`, `SUSPENDED`, or `LOCKED` states.

### FR-USER-09: Deactivate User
Set user status to `INACTIVE`. A deactivated user cannot authenticate.

### FR-USER-10: Suspend User
Set user status to `SUSPENDED`. A suspended user cannot authenticate.

### FR-USER-11: Lock User
Set user status to `LOCKED`. A locked user cannot authenticate. Lock is typically set after repeated failed login attempts (triggered by `core-auth`).

### FR-USER-12: Unlock User
Reset user status from `LOCKED` to `ACTIVE`.

### FR-USER-13: Delete User (Soft)
Soft-delete a user account. The user record remains in the database with `is_deleted = true`. All active refresh tokens for the user are revoked via `core-auth`.

### FR-USER-14: Validate Password Complexity
Service-level password validation: minimum 8 characters, at least 1 uppercase letter, 1 lowercase letter, 1 digit. Returns validation errors rather than throwing for strength-check use cases.

---

## 4. Non-Functional Requirements

- `findByEmail` must respond in under 10ms (indexed column).
- Password hashing must use BCrypt with work factor 12.
- User email must be globally unique across the platform.
- Username must be globally unique across the platform.
- The user list endpoint supports pagination (max 100 per page).
- User status transitions must be explicit. Arbitrary status changes are not permitted.

---

## 5. Business Rules

- BR-USER-01: Email address must be unique across all users, including soft-deleted users (to prevent email reuse conflicts).
- BR-USER-02: Username must be unique across all non-deleted users.
- BR-USER-03: A user must be linked to exactly one `Person`. The link cannot be changed after creation.
- BR-USER-04: A `Person` can have at most one active (non-deleted) user account.
- BR-USER-05: Passwords must meet complexity requirements before they are accepted.
- BR-USER-06: A new user is always created with `mustChangePassword = true` unless explicitly set otherwise.
- BR-USER-07: Soft-deleted users are never returned in queries and cannot authenticate.
- BR-USER-08: Status transitions are restricted: see allowed transitions table below.

### Allowed Status Transitions

| From        | To          | Trigger             |
|-------------|-------------|---------------------|
| `ACTIVE`    | `INACTIVE`  | Admin deactivates   |
| `ACTIVE`    | `SUSPENDED` | Admin suspends      |
| `ACTIVE`    | `LOCKED`    | Repeated login fails|
| `INACTIVE`  | `ACTIVE`    | Admin activates     |
| `SUSPENDED` | `ACTIVE`    | Admin activates     |
| `LOCKED`    | `ACTIVE`    | Admin unlocks       |

---

## 6. Domain Model

### User Entity

| Field              | Type           | Description                                       |
|--------------------|----------------|---------------------------------------------------|
| id                 | Long           | Surrogate PK                                      |
| personId           | Long           | FK → persons(id), NOT NULL, set once              |
| email              | String         | Unique, NOT NULL, max 255                         |
| username           | String         | Unique, NOT NULL, max 50                          |
| passwordHash       | String         | BCrypt hash, NOT NULL, never exposed in responses |
| status             | UserStatus     | Enum: ACTIVE, INACTIVE, SUSPENDED, LOCKED         |
| mustChangePassword | boolean        | Forces password change on next login              |
| lastLoginAt        | LocalDateTime  | Timestamp of last successful login, nullable      |
| isDeleted          | boolean        | Soft delete flag                                  |
| deletedAt          | LocalDateTime  | Soft delete timestamp                             |
| createdAt          | LocalDateTime  | Audit field                                       |
| updatedAt          | LocalDateTime  | Audit field                                       |
| createdBy          | Long           | Audit field                                       |
| updatedBy          | Long           | Audit field                                       |

### UserStatus Enum
- `ACTIVE` — can authenticate
- `INACTIVE` — cannot authenticate, manually deactivated
- `SUSPENDED` — cannot authenticate, temporarily suspended
- `LOCKED` — cannot authenticate, locked due to security event

---

## 7. Entity Relationships

```
persons (core-person)
    │
    │ 1:1
    ▼
users (core-user)
    │
    │ 1:N
    ▼
user_roles (core-role)
    │
    │ N:1
    ▼
roles (core-role)
```

- One user → one person (many-to-one from user's perspective; FK on users table)
- One user → many roles (via join table `user_roles`)
- `User` entity holds `personId` as a raw foreign key; it does not import `Person` as a JPA relationship to avoid cross-module coupling in certain layering scenarios — the service layer resolves Person details when needed.

---

## 8. Data Model

### Table: `users`

```sql
CREATE TABLE users (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    person_id   BIGINT UNSIGNED NOT NULL,
    email       VARCHAR(255)    NOT NULL,
    username    VARCHAR(50)     NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at DATETIME      NULL,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED NULL,
    updated_by  BIGINT UNSIGNED NULL,

    CONSTRAINT fk_users_person FOREIGN KEY (person_id) REFERENCES persons(id),
    UNIQUE uq_users_email (email),
    UNIQUE uq_users_username (username),
    INDEX idx_users_person_id (person_id),
    INDEX idx_users_status (status)
);
```

---

## 9. Package Organization

```
com.attendai.core.user
├── entity
│   ├── User.java
│   └── UserStatus.java
├── repository
│   └── UserRepository.java
├── service
│   ├── UserService.java
│   └── UserServiceImpl.java
├── controller
│   └── UserController.java
├── dto
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   ├── ChangePasswordRequest.java
│   ├── UserResponse.java
│   └── UserSummaryResponse.java
├── mapper
│   └── UserMapper.java
└── exception
    ├── UserNotFoundException.java
    └── UserAlreadyExistsException.java
```

---

## 10. API Contracts

Base path: `/api/v1/core/users`

### POST /api/v1/core/users — Create User

**Permission required:** `CORE_USER_CREATE`

**Request:**
```json
{
  "personId": 101,
  "email": "john.doe@example.com",
  "username": "john.doe",
  "temporaryPassword": "TempPass1",
  "mustChangePassword": true
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "personId": 101,
    "email": "john.doe@example.com",
    "username": "john.doe",
    "status": "ACTIVE",
    "mustChangePassword": true,
    "createdAt": "2025-01-15T10:00:00Z"
  }
}
```

---

### GET /api/v1/core/users/{id} — Get User by ID

**Permission required:** `CORE_USER_READ`

**Response 200:** `UserResponse`
**Response 404:** User not found

---

### GET /api/v1/core/users — List Users

**Permission required:** `CORE_USER_READ`

**Query parameters:** `page`, `size`, `status`, `search` (matches email or username)

**Response 200:** Paginated `UserSummaryResponse`

---

### PUT /api/v1/core/users/{id} — Update User

**Permission required:** `CORE_USER_UPDATE`

**Request:**
```json
{
  "email": "newemail@example.com",
  "username": "new.username",
  "mustChangePassword": false
}
```

**Response 200:** Updated `UserResponse`

---

### PATCH /api/v1/core/users/{id}/status — Change Status

**Permission required:** `CORE_USER_UPDATE`

**Request:**
```json
{
  "status": "INACTIVE",
  "reason": "Departed from organisation"
}
```

**Response 200:** Updated `UserResponse`

---

### POST /api/v1/core/users/{id}/change-password — Change Password

**Permission required:** Authenticated (own account), or `CORE_USER_UPDATE` (admin override)

**Request:**
```json
{
  "currentPassword": "OldPass1",
  "newPassword": "NewPass1"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Password changed successfully" }
}
```

---

### DELETE /api/v1/core/users/{id} — Soft Delete User

**Permission required:** `CORE_USER_DELETE`

**Response 204:** No content

---

## 11. Request Validation Rules

### CreateUserRequest
| Field             | Rule                                                         |
|-------------------|--------------------------------------------------------------|
| personId          | Not null, must reference existing non-deleted Person         |
| email             | Not blank, valid email, max 255, globally unique             |
| username          | Not blank, alphanumeric + dots/hyphens/underscores, max 50, unique |
| temporaryPassword | Not blank, meets password complexity rules                   |
| mustChangePassword| Optional, defaults to true                                   |

### UpdateUserRequest
| Field             | Rule                                            |
|-------------------|-------------------------------------------------|
| email             | Valid email if provided, max 255, globally unique|
| username          | Alphanumeric + dots/hyphens, max 50, unique      |

### ChangePasswordRequest
| Field           | Rule                              |
|-----------------|-----------------------------------|
| currentPassword | Not blank (required for self)     |
| newPassword     | Not blank, meets complexity rules |

### ChangeStatusRequest
| Field  | Rule                                                       |
|--------|------------------------------------------------------------|
| status | Not null, must be a valid `UserStatus` enum value          |
| reason | Optional, max 500 chars                                    |

---

## 12. Response Models

### UserResponse (full)
```json
{
  "id": 1,
  "personId": 101,
  "email": "john.doe@example.com",
  "username": "john.doe",
  "status": "ACTIVE",
  "mustChangePassword": false,
  "lastLoginAt": "2025-01-15T09:30:00Z",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-15T09:30:00Z"
}
```

`passwordHash` is never included in any response.

### UserSummaryResponse (list)
```json
{
  "id": 1,
  "personId": 101,
  "email": "john.doe@example.com",
  "username": "john.doe",
  "status": "ACTIVE"
}
```

---

## 13. Authorization

| Operation         | Required Permission  |
|-------------------|----------------------|
| Create user       | `CORE_USER_CREATE`   |
| Read user         | `CORE_USER_READ`     |
| List users        | `CORE_USER_READ`     |
| Update user       | `CORE_USER_UPDATE`   |
| Change status     | `CORE_USER_UPDATE`   |
| Change password   | Own account (any auth user) or `CORE_USER_UPDATE` |
| Delete user       | `CORE_USER_DELETE`   |

---

## 14. Service API (Internal — for core-auth)

These methods are exposed as Spring beans and called internally by `core-auth`:

```
UserService.findByEmail(String email): Optional<UserAuthProjection>
UserService.findById(Long id): Optional<UserAuthProjection>
UserService.updatePassword(Long userId, String newPasswordHash): void
UserService.updateLastLoginAt(Long userId): void
UserService.lockUser(Long userId): void
```

`UserAuthProjection` exposes only the fields `core-auth` needs: `id`, `email`, `passwordHash`, `status`, `mustChangePassword`. It does not expose the full entity.

---

## 15. Integration Points

| Module          | Integration                                                      |
|-----------------|------------------------------------------------------------------|
| `core-common`   | `BaseEntity`, `SoftDeletableEntity`, exceptions, response types  |
| `core-person`   | Validates `personId` existence before creating a user            |
| `core-auth`     | Calls `UserService.findByEmail()` during login                   |
| `core-role`     | User-role assignments are managed by `core-role`                 |
| `core-audit`    | Write audit events for user create, update, delete, status change|

---

## 16. Error Handling

| Scenario                            | Exception                       | HTTP |
|-------------------------------------|---------------------------------|------|
| User not found by ID                | `UserNotFoundException`         | 404  |
| Email already exists                | `UserAlreadyExistsException`    | 409  |
| Username already exists             | `UserAlreadyExistsException`    | 409  |
| Person not found when creating user | `ResourceNotFoundException`     | 404  |
| Person already has a user account   | `ResourceAlreadyExistsException`| 409  |
| Invalid status transition           | `ValidationException`           | 400  |
| Password complexity failure         | `ValidationException`           | 400  |
| Wrong current password              | `ValidationException`           | 400  |

---

## 17. Logging and Audit

Audit events written via `core-audit`:

| Action                | Audit Code           | Details                        |
|-----------------------|----------------------|--------------------------------|
| User created          | `USER_CREATED`       | user_id, created_by            |
| User updated          | `USER_UPDATED`       | user_id, changed fields        |
| User status changed   | `USER_STATUS_CHANGED`| user_id, old status, new status|
| User deleted          | `USER_DELETED`       | user_id                        |
| Password changed      | `USER_PASSWORD_CHANGED` | user_id                     |

Service-level logging:
- INFO: Create, update, delete, status change
- WARN: Failed status transitions, password complexity failures

---

## 18. Security Considerations

- `passwordHash` field must never be included in any DTO or API response.
- `UserAuthProjection` exposes `passwordHash` only to `core-auth` (internal Spring bean call).
- The change-password endpoint requires the current password for self-service calls. Admin overrides (via `core-auth` password reset) bypass this check at the service level.
- Soft-deleted users cannot authenticate. `UserService.findByEmail()` excludes soft-deleted records.

---

## 19. Flyway Migrations

```
V3__create_users_table.sql
```

Note: `persons` table (V-numbered before users) must be created first.

---

## 20. Testing Strategy

| Test Type       | Scope                                                            |
|-----------------|------------------------------------------------------------------|
| Unit — Service  | Create, find, update, status transitions, password change, delete |
| Unit — Service  | Invalid transitions, duplicate email/username, missing person    |
| Repository test | `findByEmail`, `findByUsername`, soft-delete filter, status filter|
| Controller test | All endpoints, HTTP status codes, validation error responses     |
| Security tests  | `CORE_USER_CREATE` required for create; 401 without token        |
| Integration     | Full create → update → deactivate → delete lifecycle             |

---

## 21. Implementation Roadmap

### Task 1: Entity and repository
- `User` entity extending `SoftDeletableEntity`
- `UserStatus` enum
- `UserRepository` with `findByEmail`, `findByUsername`, `findByPersonId`
- Flyway: `V3__create_users_table.sql`

### Task 2: Core service — read operations
- `UserService` interface + `UserServiceImpl`
- Implement `findById`, `findByEmail`, `findByUsername`, `findAll` with pagination
- Repository unit tests

### Task 3: Core service — write operations
- Implement `createUser`, `updateUser`, `changePassword`, `deleteUser`
- Implement `updateLastLoginAt`, `lockUser` (internal API for core-auth)
- Password complexity validator

### Task 4: Status management
- Implement `changeStatus` with transition validation
- Unit tests for all valid and invalid transitions

### Task 5: Controller and DTOs
- `UserController` with all endpoints
- Request/response DTOs with validation annotations
- `UserMapper` (MapStruct)
- Controller tests

### Task 6: Audit integration
- Write audit events for all write operations

---

## 22. Acceptance Criteria

- [ ] Creating a user with a duplicate email returns 409
- [ ] Creating a user with a duplicate username returns 409
- [ ] `passwordHash` never appears in any API response
- [ ] A new user always has `mustChangePassword = true` unless explicitly set
- [ ] Status transitions enforce the allowed transition table
- [ ] Soft-deleted users are excluded from all list and search queries
- [ ] `findByEmail` returns the user within 10ms (indexed lookup)
- [ ] All write operations produce audit log entries
- [ ] The `UserAuthProjection` exposes only the fields required by `core-auth`

---

## 23. Out of Scope

- Self-service user registration
- OAuth2 or external identity provider integration
- User profile avatars (belongs in `core-file`)
- Role and permission management (core-role, core-permission)
- Person data management (core-person)
