# Specification: school-administrator

## 1. Overview

`school-administrator` manages the administrator accounts for a school. An administrator is a school staff member who has platform access to manage the school's academic configuration, student/teacher records, and attendance operations.

An administrator is a school-specific domain entity that links a Core `Person` to a Core `User` within the context of a specific school. The `Person` holds identity data; the `User` holds login credentials; `SchoolAdministrator` holds the school-specific role and profile.

---

## 2. Scope and Objectives

**In scope:**
- Administrator profile creation and management within a school
- Linking an administrator to an existing Core `Person` and Core `User`
- Administrator status management (ACTIVE, INACTIVE)
- Listing administrators for a school
- Administrator soft delete

**Out of scope:**
- User account creation (delegates to `core-user`)
- Person creation (delegates to `core-person`)
- Authentication (handled by `core-auth`)
- Permission management (handled by `core-role` / `core-permission`)

---

## 3. Functional Requirements

### FR-ADMIN-01: Create Administrator
Create a school administrator by linking an existing `Person` and an existing `User` to a school. Assign the `SCHOOL_ADMIN` role to the user via `core-role`.

### FR-ADMIN-02: Get Administrator by ID
Retrieve a single non-deleted administrator for a school.

### FR-ADMIN-03: List Administrators for School
Return a paginated list of administrators belonging to a school.

### FR-ADMIN-04: Update Administrator
Update administrator profile fields: designation, notes.

### FR-ADMIN-05: Deactivate Administrator
Set administrator status to `INACTIVE`. Revokes the `SCHOOL_ADMIN` role from the user.

### FR-ADMIN-06: Activate Administrator
Set administrator status back to `ACTIVE`. Re-assigns the `SCHOOL_ADMIN` role.

### FR-ADMIN-07: Delete Administrator (Soft)
Soft-delete the administrator record. Revokes the `SCHOOL_ADMIN` role from the user.

---

## 4. Business Rules

- BR-ADMIN-01: A `Person` can be an administrator in at most one school at a time.
- BR-ADMIN-02: The `User` linked must have `ACTIVE` status in Core.
- BR-ADMIN-03: A school must have at least one `ACTIVE` administrator at all times. Deleting or deactivating the last administrator is rejected.
- BR-ADMIN-04: The `User` and `Person` must belong to the same person record (i.e., `user.personId == person.id`).
- BR-ADMIN-05: Deactivating an administrator automatically revokes the `SCHOOL_ADMIN` role.

---

## 5. Domain Model

### SchoolAdministrator Entity

| Field         | Type                 | Description                                     |
|---------------|----------------------|-------------------------------------------------|
| id            | Long                 | Surrogate PK                                    |
| schoolId      | Long                 | FK → school_schools(id), NOT NULL               |
| personId      | Long                 | FK → persons(id) [Core], NOT NULL               |
| userId        | Long                 | FK → users(id) [Core], NOT NULL                 |
| designation   | String               | Optional, e.g. "Principal", max 100             |
| status        | AdministratorStatus  | Enum: ACTIVE, INACTIVE                          |
| notes         | String               | Optional, max 500                               |
| isDeleted     | boolean              | Soft delete flag                                |
| deletedAt     | LocalDateTime        | Soft delete timestamp                           |
| createdAt     | LocalDateTime        | Audit                                           |
| updatedAt     | LocalDateTime        | Audit                                           |
| createdBy     | Long                 | Audit                                           |
| updatedBy     | Long                 | Audit                                           |

### AdministratorStatus Enum
- `ACTIVE`
- `INACTIVE`

---

## 6. Entity Relationships

```
school_schools (school-school)
    │
    │ 1:N
    ▼
school_administrators
    │
    ├── N:1 → persons (core-person)   [personId]
    └── N:1 → users (core-user)       [userId]
```

---

## 7. Data Model

### Table: `school_administrators`

```sql
CREATE TABLE school_administrators (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id    BIGINT UNSIGNED  NOT NULL,
    person_id    BIGINT UNSIGNED  NOT NULL,
    user_id      BIGINT UNSIGNED  NOT NULL,
    designation  VARCHAR(100)     NULL,
    status       VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes        VARCHAR(500)     NULL,
    is_deleted   BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME         NULL,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_admins_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_admins_person_school (person_id, school_id),
    UNIQUE uq_school_admins_user (user_id),
    INDEX idx_school_admins_school_id (school_id),
    INDEX idx_school_admins_status (status)
);
```

---

## 8. Package Organization

```
com.attendai.school.administrator
├── entity
│   ├── SchoolAdministrator.java
│   └── AdministratorStatus.java
├── repository
│   └── SchoolAdministratorRepository.java
├── service
│   ├── SchoolAdministratorService.java
│   └── SchoolAdministratorServiceImpl.java
├── controller
│   └── SchoolAdministratorController.java
├── dto
│   ├── CreateAdministratorRequest.java
│   ├── UpdateAdministratorRequest.java
│   ├── AdministratorResponse.java
│   └── AdministratorSummaryResponse.java
├── mapper
│   └── SchoolAdministratorMapper.java
└── exception
    └── AdministratorNotFoundException.java
```

---

## 9. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/administrators`

### POST — Create Administrator

**Permission:** `SCHOOL_ADMINISTRATOR_CREATE`

**Request:**
```json
{
  "personId": 101,
  "userId": 10,
  "designation": "Principal",
  "notes": null
}
```

**Response 201:** `AdministratorResponse`

---

### GET /{id} — Get Administrator

**Permission:** `SCHOOL_ADMINISTRATOR_READ`

**Response 200:** `AdministratorResponse`

---

### GET — List Administrators

**Permission:** `SCHOOL_ADMINISTRATOR_READ`

**Query params:** `page`, `size`, `status`

**Response 200:** Paginated `AdministratorSummaryResponse`

---

### PUT /{id} — Update Administrator

**Permission:** `SCHOOL_ADMINISTRATOR_UPDATE`

**Request:** `{ "designation": "Vice Principal", "notes": "..." }`

**Response 200:** `AdministratorResponse`

---

### PATCH /{id}/status — Change Status

**Permission:** `SCHOOL_ADMINISTRATOR_UPDATE`

**Request:** `{ "status": "INACTIVE" }`

**Response 200:** `AdministratorResponse`
**Response 409:** Cannot deactivate last active administrator

---

### DELETE /{id}

**Permission:** `SCHOOL_ADMINISTRATOR_DELETE`

**Response 204**
**Response 409:** Last administrator guard

---

## 10. Validation Rules

### CreateAdministratorRequest
| Field       | Rule                                                        |
|-------------|-------------------------------------------------------------|
| personId    | Not null, must reference existing non-deleted Core Person   |
| userId      | Not null, must reference active Core User                   |
| designation | Optional, max 100                                           |

---

## 11. Authorization

| Operation              | Permission                      |
|------------------------|---------------------------------|
| Create administrator   | `SCHOOL_ADMINISTRATOR_CREATE`   |
| Read administrator     | `SCHOOL_ADMINISTRATOR_READ`     |
| Update administrator   | `SCHOOL_ADMINISTRATOR_UPDATE`   |
| Change status          | `SCHOOL_ADMINISTRATOR_UPDATE`   |
| Delete administrator   | `SCHOOL_ADMINISTRATOR_DELETE`   |

---

## 12. Integration Points

| Module           | Integration                                                        |
|------------------|--------------------------------------------------------------------|
| `core-person`    | `PersonService.existsById()` — validates personId on create        |
| `core-user`      | Validates userId exists and is ACTIVE                              |
| `core-role`      | `RoleService.assignRoleToUser(userId, SCHOOL_ADMIN)` on create     |
| `core-role`      | `RoleService.removeRoleFromUser(userId, SCHOOL_ADMIN)` on deactivate/delete |
| `school-school`  | `SchoolService.isActive(schoolId)` validated before create         |
| `core-audit`     | Audit events for all write operations                              |

---

## 13. Error Handling

| Scenario                              | Exception                        | HTTP |
|---------------------------------------|----------------------------------|------|
| Administrator not found               | `AdministratorNotFoundException` | 404  |
| Person already administrator in school| `ResourceAlreadyExistsException` | 409  |
| User already linked to administrator  | `ResourceAlreadyExistsException` | 409  |
| Deactivating last administrator       | `ValidationException`            | 409  |
| Person-User mismatch                  | `ValidationException`            | 400  |

---

## 14. Flyway Migrations

```
V104__create_school_administrators_table.sql
```

---

## 15. Testing Strategy

| Test Type      | Scope                                                                   |
|----------------|-------------------------------------------------------------------------|
| Unit — Service | Create, deactivate, delete with last-admin guard                        |
| Unit — Service | Role assignment and revocation on status change                         |
| Repository test| By school, by status, soft-delete exclusion                             |
| Controller test| All endpoints, school scoping, HTTP codes                               |
| Integration    | Create admin → deactivate → verify SCHOOL_ADMIN role revoked            |

---

## 16. Acceptance Criteria

- [ ] Creating an administrator assigns `SCHOOL_ADMIN` role to the linked user
- [ ] Deactivating an administrator revokes `SCHOOL_ADMIN` role
- [ ] The last active administrator of a school cannot be deactivated or deleted
- [ ] A person cannot be administrator in more than one school simultaneously
- [ ] All write operations produce audit log entries

---

## 17. Out of Scope

- Super-admin platform roles (handled by Core permission seeding)
- Administrator payroll or HR records
