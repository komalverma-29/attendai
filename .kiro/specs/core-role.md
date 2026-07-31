# Specification: core-role

## 1. Overview

`core-role` manages the role definitions and role assignments for the AttendAI platform. A role is a named grouping of permissions. Roles are assigned to users to define what actions they are permitted to perform.

`core-role` does not make authorization decisions. It provides the data layer — role definitions, user-role assignments, and role queries — that `core-auth` reads when building JWT claims, and that `core-permission` uses to resolve the full permission set for a role.

Roles are domain-agnostic. Business modules define their own role names (e.g., `SCHOOL_ADMIN`, `SCHOOL_TEACHER`) and register them using the Core role system. Core itself defines system-level roles (e.g., `SYSTEM_ADMIN`).

---

## 2. Scope and Objectives

**In scope:**
- Role definition CRUD (create, read, update, soft-delete)
- Role assignment to users
- Role removal from users
- Listing roles assigned to a user
- Listing users assigned to a role
- System role seeding (non-deletable built-in roles)
- Role-to-permission mapping (stored here; queried by `core-permission`)

**Out of scope:**
- Authorization enforcement (Spring Security + `core-auth`)
- Permission definitions (belongs in `core-permission`)
- Business-domain role concepts (school admin, teacher — seeded by business modules)

---

## 3. Functional Requirements

### FR-ROLE-01: Create Role
Create a named role with a code, display name, and optional description. Role codes are unique and immutable after creation.

### FR-ROLE-02: Get Role by ID
Retrieve a single role by its surrogate ID.

### FR-ROLE-03: Get Role by Code
Retrieve a single role by its unique code string. Used by `core-auth` during token building.

### FR-ROLE-04: List Roles
Return a paginated list of all non-deleted roles. Supports optional search by name or code.

### FR-ROLE-05: Update Role
Update the display name and description. Role codes are immutable after creation.

### FR-ROLE-06: Delete Role (Soft)
Soft-delete a role. A role cannot be deleted if it is currently assigned to any user.

### FR-ROLE-07: Assign Role to User
Create a `user_roles` assignment record linking a user to a role. A user can hold multiple roles. Duplicate assignments are rejected.

### FR-ROLE-08: Remove Role from User
Remove the `user_roles` assignment record for a specific user-role pair.

### FR-ROLE-09: Get Roles for User
Return all roles assigned to a given user. Used by `core-auth` during login and token refresh.

### FR-ROLE-10: Get Users for Role
Return a paginated list of users assigned to a specific role.

### FR-ROLE-11: System Role Seeding
On application startup, predefined system roles are seeded into the database if they do not already exist. System roles are marked as `isSystem = true` and cannot be deleted or have their code changed.

---

## 4. Non-Functional Requirements

- Role code lookup must use an indexed column for O(1) performance.
- Role assignment lookup (get roles for user) must be efficient — called on every login.
- Role codes are uppercase, snake_case strings (e.g., `SYSTEM_ADMIN`, `SCHOOL_TEACHER`).
- A maximum of 50 roles can be assigned to a single user (guard against abuse).
- System roles cannot be deleted or have `isSystem` set to `false`.

---

## 5. Business Rules

- BR-ROLE-01: Role codes must be unique, uppercase, and contain only letters, digits, and underscores.
- BR-ROLE-02: Role codes are immutable after creation.
- BR-ROLE-03: System roles (`isSystem = true`) cannot be deleted.
- BR-ROLE-04: A role cannot be deleted if it has active user assignments.
- BR-ROLE-05: A user may not be assigned the same role more than once.
- BR-ROLE-06: Role-permission associations are managed by `core-permission`, not `core-role`. `core-role` stores the join table `role_permissions` but the association is modified through `core-permission`.

---

## 6. Domain Model

### Role Entity

| Field       | Type          | Description                                              |
|-------------|---------------|----------------------------------------------------------|
| id          | Long          | Surrogate PK                                             |
| code        | String        | Unique, uppercase, immutable, max 100, e.g. `SYSTEM_ADMIN` |
| name        | String        | Display name, max 255                                    |
| description | String        | Optional, max 1000                                       |
| isSystem    | boolean       | True for built-in non-deletable roles                    |
| isDeleted   | boolean       | Soft delete flag                                         |
| deletedAt   | LocalDateTime | Soft delete timestamp                                    |
| createdAt   | LocalDateTime | Audit                                                    |
| updatedAt   | LocalDateTime | Audit                                                    |
| createdBy   | Long          | Audit                                                    |
| updatedBy   | Long          | Audit                                                    |

### UserRole (join entity)

| Field      | Type          | Description                         |
|------------|---------------|-------------------------------------|
| id         | Long          | Surrogate PK                        |
| userId     | Long          | FK → users(id), NOT NULL            |
| roleId     | Long          | FK → roles(id), NOT NULL            |
| assignedAt | LocalDateTime | Timestamp of assignment             |
| assignedBy | Long          | User who made the assignment        |

---

## 7. Entity Relationships

```
users (core-user)
    │
    │ M:N (via user_roles)
    ▼
roles (core-role)
    │
    │ M:N (via role_permissions)
    ▼
permissions (core-permission)
```

---

## 8. Data Model

### Table: `roles`

```sql
CREATE TABLE roles (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(100)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    description TEXT            NULL,
    is_system   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED NULL,
    updated_by  BIGINT UNSIGNED NULL,

    UNIQUE uq_roles_code (code),
    INDEX idx_roles_is_deleted (is_deleted)
);
```

### Table: `user_roles`

```sql
CREATE TABLE user_roles (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED NOT NULL,
    role_id     BIGINT UNSIGNED NOT NULL,
    assigned_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT UNSIGNED NULL,

    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE uq_user_roles (user_id, role_id),
    INDEX idx_user_roles_user_id (user_id),
    INDEX idx_user_roles_role_id (role_id)
);
```

---

## 9. Package Organization

```
com.attendai.core.role
├── entity
│   ├── Role.java
│   └── UserRole.java
├── repository
│   ├── RoleRepository.java
│   └── UserRoleRepository.java
├── service
│   ├── RoleService.java
│   └── RoleServiceImpl.java
├── controller
│   └── RoleController.java
├── dto
│   ├── CreateRoleRequest.java
│   ├── UpdateRoleRequest.java
│   ├── AssignRoleRequest.java
│   ├── RoleResponse.java
│   └── RoleSummaryResponse.java
├── mapper
│   └── RoleMapper.java
└── exception
    ├── RoleNotFoundException.java
    └── RoleAlreadyExistsException.java
```

---

## 10. System Role Seeds

The following roles are seeded by `core-role` on startup:

| Code           | Name             | Description                               |
|----------------|------------------|-------------------------------------------|
| `SYSTEM_ADMIN` | System Admin     | Full platform access; system-level only   |

Business modules seed their own domain roles (e.g., `SCHOOL_ADMIN`, `SCHOOL_TEACHER`) during their own startup. Core does not define business module roles.

---

## 11. API Contracts

Base path: `/api/v1/core/roles`

### POST /api/v1/core/roles — Create Role

**Permission:** `CORE_ROLE_CREATE`

**Request:**
```json
{
  "code": "REPORT_VIEWER",
  "name": "Report Viewer",
  "description": "Can view all reports"
}
```

**Response 201:** `RoleResponse`

---

### GET /api/v1/core/roles/{id} — Get Role

**Permission:** `CORE_ROLE_READ`

**Response 200:** `RoleResponse`

---

### GET /api/v1/core/roles — List Roles

**Permission:** `CORE_ROLE_READ`

**Query params:** `page`, `size`, `search`

**Response 200:** Paginated `RoleSummaryResponse`

---

### PUT /api/v1/core/roles/{id} — Update Role

**Permission:** `CORE_ROLE_UPDATE`

**Request:**
```json
{
  "name": "Updated Name",
  "description": "Updated description"
}
```

**Response 200:** `RoleResponse`

---

### DELETE /api/v1/core/roles/{id} — Delete Role

**Permission:** `CORE_ROLE_DELETE`

**Response 204:** No content
**Response 409:** Role has active user assignments

---

### POST /api/v1/core/users/{userId}/roles — Assign Role to User

**Permission:** `CORE_ROLE_ASSIGN`

**Request:**
```json
{
  "roleId": 3
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Role assigned successfully" }
}
```

---

### DELETE /api/v1/core/users/{userId}/roles/{roleId} — Remove Role from User

**Permission:** `CORE_ROLE_ASSIGN`

**Response 204:** No content

---

### GET /api/v1/core/users/{userId}/roles — Get Roles for User

**Permission:** `CORE_ROLE_READ`

**Response 200:** List of `RoleSummaryResponse`

---

### GET /api/v1/core/roles/{roleId}/users — Get Users for Role

**Permission:** `CORE_ROLE_READ`

**Response 200:** Paginated list of user summaries

---

## 12. Request Validation Rules

### CreateRoleRequest
| Field       | Rule                                                               |
|-------------|--------------------------------------------------------------------|
| code        | Not blank, max 100, uppercase, letters/digits/underscores only, unique |
| name        | Not blank, max 255                                                 |
| description | Optional, max 1000                                                 |

### UpdateRoleRequest
| Field       | Rule                   |
|-------------|------------------------|
| name        | Not blank if present, max 255 |
| description | Optional, max 1000     |

### AssignRoleRequest
| Field  | Rule                            |
|--------|---------------------------------|
| roleId | Not null, must exist in DB      |

---

## 13. Authorization

| Operation             | Required Permission  |
|-----------------------|----------------------|
| Create role           | `CORE_ROLE_CREATE`   |
| Read role / list      | `CORE_ROLE_READ`     |
| Update role           | `CORE_ROLE_UPDATE`   |
| Delete role           | `CORE_ROLE_DELETE`   |
| Assign/remove role    | `CORE_ROLE_ASSIGN`   |
| Get roles for user    | `CORE_ROLE_READ`     |

---

## 14. Internal Service API

Methods exposed as Spring beans to other Core modules:

```
RoleService.findRolesByUserId(Long userId): List<RoleResponse>
RoleService.findByCode(String code): Optional<RoleResponse>
RoleService.existsById(Long id): boolean
```

Used by `core-auth` to build JWT claims during login and token refresh.

---

## 15. Integration Points

| Module           | Integration                                               |
|------------------|-----------------------------------------------------------|
| `core-common`    | Base entities, exceptions, response types                 |
| `core-user`      | `userId` references in `user_roles`                       |
| `core-permission`| `role_permissions` join table is queried by `core-permission` |
| `core-auth`      | `findRolesByUserId` called during token creation          |
| `core-audit`     | Audit events for role CRUD and role assignments           |

---

## 16. Error Handling

| Scenario                            | Exception                       | HTTP |
|-------------------------------------|---------------------------------|------|
| Role not found                      | `RoleNotFoundException`         | 404  |
| Role code already exists            | `RoleAlreadyExistsException`    | 409  |
| Deleting a system role              | `ValidationException`           | 400  |
| Deleting a role with assignments    | `ValidationException`           | 409  |
| Duplicate role assignment           | `ResourceAlreadyExistsException`| 409  |
| User not found on role assign       | `ResourceNotFoundException`     | 404  |

---

## 17. Logging and Audit

Audit events written via `core-audit`:

| Action               | Audit Code             | Details                    |
|----------------------|------------------------|----------------------------|
| Role created         | `ROLE_CREATED`         | role_id, code              |
| Role updated         | `ROLE_UPDATED`         | role_id, changed fields    |
| Role deleted         | `ROLE_DELETED`         | role_id                    |
| Role assigned        | `ROLE_ASSIGNED_TO_USER`| role_id, user_id           |
| Role removed         | `ROLE_REMOVED_FROM_USER`| role_id, user_id          |

---

## 18. Flyway Migrations

```
V4__create_roles_table.sql
V5__create_user_roles_table.sql
V6__seed_system_roles.sql
```

---

## 19. Testing Strategy

| Test Type       | Scope                                                              |
|-----------------|--------------------------------------------------------------------|
| Unit — Service  | Create, update, delete (including system role protection)          |
| Unit — Service  | Assign role, remove role, duplicate assignment rejection           |
| Repository test | `findByCode`, `findByUserId`, soft-delete exclusion                |
| Controller test | All endpoints, HTTP codes, validation, pagination                  |
| Security tests  | `CORE_ROLE_CREATE` required; 401 without token                     |
| Integration     | Full role create → assign to user → login sees role in JWT claims  |

---

## 20. Implementation Roadmap

### Task 1: Role entity and repository
- `Role` entity, `UserRole` entity
- `RoleRepository`, `UserRoleRepository`
- Flyway: `V4`, `V5`, `V6`

### Task 2: Service — CRUD
- `createRole`, `findById`, `findByCode`, `listRoles`, `updateRole`, `deleteRole`
- System role protection in delete

### Task 3: Service — assignments
- `assignRoleToUser`, `removeRoleFromUser`
- `findRolesByUserId`, `findUsersByRoleId`

### Task 4: Controller and DTOs
- `RoleController`, all endpoints
- `RoleMapper`, DTOs

### Task 5: Startup seeding
- `ApplicationListener` or `CommandLineRunner` that seeds system roles

### Task 6: Audit integration
- Write audit events for all write operations

---

## 21. Acceptance Criteria

- [ ] Role code is immutable after creation
- [ ] System roles cannot be deleted
- [ ] A role with active user assignments cannot be deleted
- [ ] Duplicate user-role assignments are rejected with 409
- [ ] `findRolesByUserId` returns correct roles for JWT building
- [ ] System roles are present in the database on first startup
- [ ] All role CRUD operations produce audit log entries
- [ ] Role codes enforce uppercase + underscore pattern at validation

---

## 22. Out of Scope

- Permission definitions (core-permission)
- Authorization enforcement (core-auth)
- Business-domain role definitions (seeded by business modules, not core-role)
- Hierarchical roles (no role inheritance in V1)
