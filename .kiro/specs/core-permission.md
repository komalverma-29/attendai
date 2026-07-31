# Specification: core-permission

## 1. Overview

`core-permission` manages the permission definitions and the role-permission assignments that form the authorization model for the AttendAI platform.

A permission is a named capability code (e.g., `CORE_USER_CREATE`, `SCHOOL_STUDENT_READ`). Permissions are assigned to roles. When a user authenticates, `core-auth` resolves the user's roles and then resolves all permissions for those roles, embedding them in the JWT claims.

Spring Security enforces authorization using these permission codes via `@PreAuthorize("hasAuthority('PERMISSION_CODE')")` at the method level.

`core-permission` does not make authorization decisions. It provides the data and the service API that the rest of the system depends on to know what permissions exist and which roles hold them.

---

## 2. Scope and Objectives

**In scope:**
- Permission definition CRUD (create, read, update, soft-delete)
- Assigning permissions to roles
- Removing permissions from roles
- Listing permissions assigned to a role
- Listing roles that hold a given permission
- System permission seeding (built-in platform permissions)
- Permission namespace enforcement (`<MODULE>_<RESOURCE>_<ACTION>` pattern)

**Out of scope:**
- Authorization enforcement (Spring Security, `core-auth`)
- Role definitions (belongs in `core-role`)
- User-role assignments (belongs in `core-role`)
- Business module permissions beyond defining the pattern; business modules seed their own permission codes

---

## 3. Functional Requirements

### FR-PERM-01: Create Permission
Create a named permission with a unique code, display name, module namespace, and optional description.

### FR-PERM-02: Get Permission by ID
Retrieve a single permission by its surrogate ID.

### FR-PERM-03: Get Permission by Code
Retrieve a single permission by its unique code string. Used by `core-auth` during token building.

### FR-PERM-04: List Permissions
Return a paginated list of all non-deleted permissions. Supports filtering by module namespace.

### FR-PERM-05: Update Permission
Update the display name and description. Permission codes are immutable after creation.

### FR-PERM-06: Delete Permission (Soft)
Soft-delete a permission. A permission cannot be deleted if it is currently assigned to any role.

### FR-PERM-07: Assign Permission to Role
Create a `role_permissions` record linking a role to a permission. Duplicate assignments are rejected.

### FR-PERM-08: Remove Permission from Role
Remove the `role_permissions` record for a specific role-permission pair.

### FR-PERM-09: Get Permissions for Role
Return all permissions assigned to a given role. Used by `core-auth` during token building.

### FR-PERM-10: Get Permissions for User (Resolved)
Resolve all permissions for a user by aggregating permissions from all of the user's roles. This is the full permission set embedded in the JWT.

### FR-PERM-11: System Permission Seeding
On application startup, Core-level permissions are seeded into the database if they do not already exist. System permissions are marked as `isSystem = true` and cannot be deleted.

---

## 4. Non-Functional Requirements

- Permission code lookup must use an indexed column.
- `getPermissionsForUser()` is called on every login and token refresh. It must be efficient — resolved in a single query with a join, not N+1 lookups.
- Permission codes must follow the `<MODULE>_<RESOURCE>_<ACTION>` naming pattern, enforced by validation.
- A maximum of 200 permissions can be assigned to a single role.
- Permission codes are uppercase only.

---

## 5. Business Rules

- BR-PERM-01: Permission codes must be unique, uppercase, and match the pattern `[A-Z0-9_]+`.
- BR-PERM-02: Permission codes are immutable after creation.
- BR-PERM-03: System permissions (`isSystem = true`) cannot be deleted.
- BR-PERM-04: A permission cannot be deleted if it is currently assigned to any role.
- BR-PERM-05: The same permission cannot be assigned to the same role more than once.
- BR-PERM-06: Permission codes should follow the convention `<MODULE>_<RESOURCE>_<ACTION>`. This is validated as a naming guideline but not enforced at the database level.

---

## 6. Domain Model

### Permission Entity

| Field       | Type          | Description                                                        |
|-------------|---------------|--------------------------------------------------------------------|
| id          | Long          | Surrogate PK                                                       |
| code        | String        | Unique, uppercase, immutable, max 100                              |
| name        | String        | Display name, max 255                                              |
| module      | String        | Namespace identifier, e.g. `CORE`, `SCHOOL`, max 50               |
| description | String        | Optional, max 1000                                                 |
| isSystem    | boolean       | True for built-in non-deletable permissions                        |
| isDeleted   | boolean       | Soft delete flag                                                   |
| deletedAt   | LocalDateTime | Soft delete timestamp                                              |
| createdAt   | LocalDateTime | Audit                                                              |
| updatedAt   | LocalDateTime | Audit                                                              |
| createdBy   | Long          | Audit                                                              |
| updatedBy   | Long          | Audit                                                              |

### RolePermission (join entity)

| Field        | Type          | Description                              |
|--------------|---------------|------------------------------------------|
| id           | Long          | Surrogate PK                             |
| roleId       | Long          | FK → roles(id), NOT NULL                 |
| permissionId | Long          | FK → permissions(id), NOT NULL           |
| assignedAt   | LocalDateTime | Timestamp of assignment                  |
| assignedBy   | Long          | User who made the assignment             |

---

## 7. Entity Relationships

```
roles (core-role)
    │
    │ M:N (via role_permissions)
    ▼
permissions (core-permission)
```

---

## 8. Data Model

### Table: `permissions`

```sql
CREATE TABLE permissions (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(100)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    module      VARCHAR(50)     NOT NULL,
    description TEXT            NULL,
    is_system   BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED NULL,
    updated_by  BIGINT UNSIGNED NULL,

    UNIQUE uq_permissions_code (code),
    INDEX idx_permissions_module (module),
    INDEX idx_permissions_is_deleted (is_deleted)
);
```

### Table: `role_permissions`

```sql
CREATE TABLE role_permissions (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    assigned_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by   BIGINT UNSIGNED NULL,

    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    UNIQUE uq_role_permissions (role_id, permission_id),
    INDEX idx_role_permissions_role_id (role_id),
    INDEX idx_role_permissions_permission_id (permission_id)
);
```

---

## 9. System Permission Seeds

The following Core-level permissions are seeded on startup:

### CORE_USER_*
| Code               | Description                |
|--------------------|----------------------------|
| `CORE_USER_CREATE` | Create a new user account  |
| `CORE_USER_READ`   | Read user details          |
| `CORE_USER_UPDATE` | Update a user account      |
| `CORE_USER_DELETE` | Delete a user account      |

### CORE_ROLE_*
| Code               | Description                |
|--------------------|----------------------------|
| `CORE_ROLE_CREATE` | Create a new role          |
| `CORE_ROLE_READ`   | Read role details          |
| `CORE_ROLE_UPDATE` | Update a role              |
| `CORE_ROLE_DELETE` | Delete a role              |
| `CORE_ROLE_ASSIGN` | Assign/remove roles to users |

### CORE_PERMISSION_*
| Code                     | Description                     |
|--------------------------|---------------------------------|
| `CORE_PERMISSION_CREATE` | Create a new permission         |
| `CORE_PERMISSION_READ`   | Read permission details         |
| `CORE_PERMISSION_UPDATE` | Update a permission             |
| `CORE_PERMISSION_DELETE` | Delete a permission             |
| `CORE_PERMISSION_ASSIGN` | Assign/remove permissions to roles |

### CORE_PERSON_*
| Code                  | Description           |
|-----------------------|-----------------------|
| `CORE_PERSON_CREATE`  | Create a person       |
| `CORE_PERSON_READ`    | Read person details   |
| `CORE_PERSON_UPDATE`  | Update a person       |
| `CORE_PERSON_DELETE`  | Delete a person       |

### CORE_AUDIT_*
| Code               | Description              |
|--------------------|--------------------------|
| `CORE_AUDIT_READ`  | Read audit logs          |

### CORE_CONFIG_*
| Code                | Description                 |
|---------------------|-----------------------------|
| `CORE_CONFIG_READ`  | Read system configuration   |
| `CORE_CONFIG_WRITE` | Write system configuration  |

### CORE_STATION_*
| Code                  | Description               |
|-----------------------|---------------------------|
| `CORE_STATION_CREATE` | Register a new station    |
| `CORE_STATION_READ`   | Read station details      |
| `CORE_STATION_UPDATE` | Update a station          |
| `CORE_STATION_DELETE` | Delete a station          |

### CORE_NOTIFICATION_*
| Code                        | Description                   |
|-----------------------------|-------------------------------|
| `CORE_NOTIFICATION_READ`    | Read notifications            |
| `CORE_NOTIFICATION_MANAGE`  | Manage notification templates |

### CORE_FILE_*
| Code              | Description       |
|-------------------|-------------------|
| `CORE_FILE_UPLOAD`| Upload a file     |
| `CORE_FILE_READ`  | Read/download a file |
| `CORE_FILE_DELETE`| Delete a file     |

---

## 10. Package Organization

```
com.attendai.core.permission
├── entity
│   ├── Permission.java
│   └── RolePermission.java
├── repository
│   ├── PermissionRepository.java
│   └── RolePermissionRepository.java
├── service
│   ├── PermissionService.java
│   └── PermissionServiceImpl.java
├── controller
│   └── PermissionController.java
├── dto
│   ├── CreatePermissionRequest.java
│   ├── UpdatePermissionRequest.java
│   ├── AssignPermissionRequest.java
│   ├── PermissionResponse.java
│   └── PermissionSummaryResponse.java
├── mapper
│   └── PermissionMapper.java
└── exception
    ├── PermissionNotFoundException.java
    └── PermissionAlreadyExistsException.java
```

---

## 11. API Contracts

Base path: `/api/v1/core/permissions`

### POST /api/v1/core/permissions — Create Permission

**Permission:** `CORE_PERMISSION_CREATE`

**Request:**
```json
{
  "code": "SCHOOL_STUDENT_CREATE",
  "name": "Create Student",
  "module": "SCHOOL",
  "description": "Allows creating a new student record"
}
```

**Response 201:** `PermissionResponse`

---

### GET /api/v1/core/permissions/{id}

**Permission:** `CORE_PERMISSION_READ`

**Response 200:** `PermissionResponse`

---

### GET /api/v1/core/permissions

**Permission:** `CORE_PERMISSION_READ`

**Query params:** `page`, `size`, `module`, `search`

**Response 200:** Paginated `PermissionSummaryResponse`

---

### PUT /api/v1/core/permissions/{id}

**Permission:** `CORE_PERMISSION_UPDATE`

**Request:**
```json
{
  "name": "Updated Name",
  "description": "Updated description"
}
```

**Response 200:** `PermissionResponse`

---

### DELETE /api/v1/core/permissions/{id}

**Permission:** `CORE_PERMISSION_DELETE`

**Response 204**
**Response 409:** Permission has active role assignments

---

### POST /api/v1/core/roles/{roleId}/permissions — Assign Permission to Role

**Permission:** `CORE_PERMISSION_ASSIGN`

**Request:**
```json
{
  "permissionId": 5
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Permission assigned to role successfully" }
}
```

---

### DELETE /api/v1/core/roles/{roleId}/permissions/{permissionId} — Remove Permission from Role

**Permission:** `CORE_PERMISSION_ASSIGN`

**Response 204**

---

### GET /api/v1/core/roles/{roleId}/permissions — Get Permissions for Role

**Permission:** `CORE_PERMISSION_READ`

**Response 200:** List of `PermissionSummaryResponse`

---

## 12. Request Validation Rules

### CreatePermissionRequest
| Field       | Rule                                                                 |
|-------------|----------------------------------------------------------------------|
| code        | Not blank, max 100, uppercase, `[A-Z0-9_]+` pattern, unique         |
| name        | Not blank, max 255                                                   |
| module      | Not blank, max 50, uppercase letters only                            |
| description | Optional, max 1000                                                   |

### AssignPermissionRequest
| Field        | Rule                      |
|--------------|---------------------------|
| permissionId | Not null, must exist in DB|

---

## 13. Authorization

| Operation                    | Required Permission        |
|------------------------------|----------------------------|
| Create permission            | `CORE_PERMISSION_CREATE`   |
| Read permission / list       | `CORE_PERMISSION_READ`     |
| Update permission            | `CORE_PERMISSION_UPDATE`   |
| Delete permission            | `CORE_PERMISSION_DELETE`   |
| Assign/remove to role        | `CORE_PERMISSION_ASSIGN`   |
| Get permissions for role     | `CORE_PERMISSION_READ`     |

---

## 14. Internal Service API

Methods exposed as Spring beans:

```
PermissionService.findPermissionsByRoleId(Long roleId): List<String>
  // Returns list of permission codes — used by core-auth for JWT building

PermissionService.findPermissionsByUserId(Long userId): List<String>
  // Resolves all permissions across all roles of a user — used by core-auth

PermissionService.findByCode(String code): Optional<PermissionResponse>

PermissionService.existsById(Long id): boolean
```

The `findPermissionsByUserId` implementation must use a single JOIN query:
```sql
SELECT DISTINCT p.code
FROM permissions p
JOIN role_permissions rp ON rp.permission_id = p.id
JOIN user_roles ur ON ur.role_id = rp.role_id
WHERE ur.user_id = :userId
  AND p.is_deleted = false
```

---

## 15. Integration Points

| Module           | Integration                                              |
|------------------|----------------------------------------------------------|
| `core-common`    | Base entities, exceptions, response types                |
| `core-role`      | `role_permissions` FK references `roles(id)`             |
| `core-auth`      | `findPermissionsByUserId` called during token creation   |
| `core-audit`     | Audit events for permission CRUD and assignments         |

---

## 16. Error Handling

| Scenario                                 | Exception                         | HTTP |
|------------------------------------------|-----------------------------------|------|
| Permission not found                     | `PermissionNotFoundException`     | 404  |
| Permission code already exists           | `PermissionAlreadyExistsException`| 409  |
| Deleting a system permission             | `ValidationException`             | 400  |
| Deleting permission with role assignments| `ValidationException`             | 409  |
| Duplicate role-permission assignment     | `ResourceAlreadyExistsException`  | 409  |
| Role not found on assignment             | `ResourceNotFoundException`       | 404  |

---

## 17. Logging and Audit

| Action                         | Audit Code                    | Details               |
|--------------------------------|-------------------------------|-----------------------|
| Permission created             | `PERMISSION_CREATED`          | permission_id, code   |
| Permission updated             | `PERMISSION_UPDATED`          | permission_id         |
| Permission deleted             | `PERMISSION_DELETED`          | permission_id         |
| Permission assigned to role    | `PERMISSION_ASSIGNED_TO_ROLE` | permission_id, role_id|
| Permission removed from role   | `PERMISSION_REMOVED_FROM_ROLE`| permission_id, role_id|

---

## 18. Flyway Migrations

```
V7__create_permissions_table.sql
V8__create_role_permissions_table.sql
V9__seed_core_permissions.sql
V10__assign_core_permissions_to_system_admin.sql
```

---

## 19. Testing Strategy

| Test Type       | Scope                                                                    |
|-----------------|--------------------------------------------------------------------------|
| Unit — Service  | Create, update, delete (system permission protection)                    |
| Unit — Service  | Assign to role, remove, duplicate rejection                              |
| Unit — Service  | `findPermissionsByUserId` — correct aggregation across multiple roles    |
| Repository test | `findByCode`, `findByRoleId`, JOIN query for user permissions            |
| Controller test | All endpoints, HTTP codes, validation, auth                              |
| Integration     | User with 2 roles → token contains all permissions from both roles       |

---

## 20. Implementation Roadmap

### Task 1: Entity and repository
- `Permission` entity, `RolePermission` entity
- `PermissionRepository`, `RolePermissionRepository`
- Flyway: `V7`, `V8`

### Task 2: Service — CRUD
- `createPermission`, `findById`, `findByCode`, `listPermissions`, `updatePermission`, `deletePermission`
- System permission protection

### Task 3: Service — assignments
- `assignPermissionToRole`, `removePermissionFromRole`
- `findPermissionsByRoleId`, `findPermissionsByUserId` (single JOIN query)

### Task 4: Controller and DTOs
- `PermissionController`, all endpoints
- `PermissionMapper`, DTOs

### Task 5: Startup seeding
- Seed all Core system permissions on startup
- Assign all Core permissions to `SYSTEM_ADMIN` role

### Task 6: Audit integration

---

## 21. Acceptance Criteria

- [ ] Permission code is immutable after creation
- [ ] System permissions cannot be deleted
- [ ] A permission with active role assignments cannot be deleted
- [ ] Duplicate role-permission assignments are rejected with 409
- [ ] `findPermissionsByUserId` returns the union of all permissions from all user roles
- [ ] Core system permissions are present in the database on first startup
- [ ] `SYSTEM_ADMIN` role has all Core permissions assigned on first startup
- [ ] All operations produce audit log entries

---

## 22. Out of Scope

- Authorization enforcement (core-auth, Spring Security)
- Role definitions (core-role)
- Business module permission definitions (seeded by business modules)
- Permission groups or categories beyond the module namespace field
- Fine-grained resource-level permissions (row-level security)
