# Specification: school-school

## 1. Overview

`school-school` is the root entity of the AttendAI School module. It represents a registered school on the platform — the organizational unit that owns administrators, teachers, students, academic years, classes, sections, and all attendance data.

Every piece of school-specific data is scoped to a school. A school is the top-level tenant boundary within `attendai-school`.

---

## 2. Scope and Objectives

**In scope:**
- School registration and CRUD
- School profile management (name, address, contact, type, logo)
- School status management (ACTIVE, INACTIVE, SUSPENDED)
- School code generation (unique short identifier)
- School logo file management (via `core-file`)
- Listing and searching schools (platform-level admin view)

**Out of scope:**
- Multi-branch or campus management (V1: one school = one entity)
- School fee structures, admissions, exams, library
- Academic year and calendar management (belongs in `school-academic-year` and `school-academic-calendar`)

---

## 3. Functional Requirements

### FR-SCHOOL-01: Register School
Create a new school record with a name, type, address, contact details, and an auto-generated unique school code.

### FR-SCHOOL-02: Get School by ID
Retrieve a single non-deleted school by its surrogate ID.

### FR-SCHOOL-03: Get School by Code
Retrieve a school by its unique short code.

### FR-SCHOOL-04: List Schools
Return a paginated list of schools. Supports search by name and filter by status and type. (Platform-admin use only.)

### FR-SCHOOL-05: Update School
Update school profile fields: name, type, address, contact details, description, logo.

### FR-SCHOOL-06: Change School Status
Transition school between ACTIVE, INACTIVE, SUSPENDED. An INACTIVE or SUSPENDED school cannot process attendance.

### FR-SCHOOL-07: Upload School Logo
Upload a school logo image via `core-file`. Stores the file ID reference on the school record.

### FR-SCHOOL-08: Delete School (Soft)
Soft-delete a school. A school with active students or teachers cannot be deleted.

---

## 4. Non-Functional Requirements

- School code must be uppercase, 4–10 alphanumeric characters, auto-generated if not provided.
- School name must be unique across non-deleted schools.
- All downstream operations (attendance, enrollment) must validate school status before proceeding.

---

## 5. Business Rules

- BR-SCHOOL-01: A school must be `ACTIVE` to allow attendance processing, student enrollment, and timetable management.
- BR-SCHOOL-02: School code is auto-generated from the first letters of the school name if not explicitly provided. It is immutable after creation.
- BR-SCHOOL-03: School name is unique across all non-deleted schools.
- BR-SCHOOL-04: A school with active (non-deleted) students or teachers cannot be soft-deleted.
- BR-SCHOOL-05: Suspending a school does not delete any data; it only prevents new attendance and enrollment operations.

---

## 6. Domain Model

### School Entity

| Field          | Type          | Description                                             |
|----------------|---------------|---------------------------------------------------------|
| id             | Long          | Surrogate PK                                            |
| name           | String        | NOT NULL, unique, max 255                               |
| code           | String        | Unique, uppercase, max 10, immutable after creation     |
| type           | SchoolType    | Enum: PRIMARY, SECONDARY, HIGHER_SECONDARY, COMBINED    |
| status         | SchoolStatus  | Enum: ACTIVE, INACTIVE, SUSPENDED                       |
| description    | String        | Optional, max 1000                                      |
| addressLine1   | String        | NOT NULL, max 255                                       |
| addressLine2   | String        | Optional, max 255                                       |
| city           | String        | NOT NULL, max 100                                       |
| stateOrProvince| String        | NOT NULL, max 100                                       |
| postalCode     | String        | Optional, max 20                                        |
| country        | String        | NOT NULL, ISO alpha-2, max 2                            |
| phone          | String        | Optional, max 30                                        |
| email          | String        | Optional, valid email, max 255                          |
| website        | String        | Optional, max 500                                       |
| logoFileId     | Long          | Optional, FK → files(id)                                |
| isDeleted      | boolean       | Soft delete flag                                        |
| deletedAt      | LocalDateTime | Soft delete timestamp                                   |
| createdAt      | LocalDateTime | Audit                                                   |
| updatedAt      | LocalDateTime | Audit                                                   |
| createdBy      | Long          | Audit                                                   |
| updatedBy      | Long          | Audit                                                   |

### SchoolType Enum
- `PRIMARY` — grades 1–5
- `SECONDARY` — grades 6–10
- `HIGHER_SECONDARY` — grades 11–12
- `COMBINED` — grades 1–12

### SchoolStatus Enum
- `ACTIVE` — fully operational
- `INACTIVE` — manually deactivated
- `SUSPENDED` — platform-level suspension

---

## 7. School Status State Machine

```
[Created] → ACTIVE
ACTIVE    → INACTIVE    (admin deactivates)
ACTIVE    → SUSPENDED   (platform admin)
INACTIVE  → ACTIVE      (admin activates)
SUSPENDED → ACTIVE      (platform admin)
Any       → [Deleted]   (soft delete, if no active students/teachers)
```

---

## 8. Data Model

### Table: `school_schools`

```sql
CREATE TABLE school_schools (
    id              BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255)     NOT NULL,
    code            VARCHAR(10)      NOT NULL,
    type            VARCHAR(20)      NOT NULL,
    status          VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    description     TEXT             NULL,
    address_line1   VARCHAR(255)     NOT NULL,
    address_line2   VARCHAR(255)     NULL,
    city            VARCHAR(100)     NOT NULL,
    state_or_province VARCHAR(100)   NOT NULL,
    postal_code     VARCHAR(20)      NULL,
    country         VARCHAR(2)       NOT NULL,
    phone           VARCHAR(30)      NULL,
    email           VARCHAR(255)     NULL,
    website         VARCHAR(500)     NULL,
    logo_file_id    BIGINT UNSIGNED  NULL,
    is_deleted      BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME         NULL,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      BIGINT UNSIGNED  NULL,
    updated_by      BIGINT UNSIGNED  NULL,

    UNIQUE uq_school_schools_name (name),
    UNIQUE uq_school_schools_code (code),
    INDEX idx_school_schools_status (status)
);
```

---

## 9. Package Organization

```
com.attendai.school.school
├── entity
│   ├── School.java
│   ├── SchoolType.java
│   └── SchoolStatus.java
├── repository
│   └── SchoolRepository.java
├── service
│   ├── SchoolService.java
│   └── SchoolServiceImpl.java
├── controller
│   └── SchoolController.java
├── dto
│   ├── CreateSchoolRequest.java
│   ├── UpdateSchoolRequest.java
│   ├── ChangeSchoolStatusRequest.java
│   ├── SchoolResponse.java
│   └── SchoolSummaryResponse.java
├── mapper
│   └── SchoolMapper.java
└── exception
    ├── SchoolNotFoundException.java
    └── SchoolAlreadyExistsException.java
```

---

## 10. API Contracts

Base path: `/api/v1/school/schools`

### POST /api/v1/school/schools

**Permission:** `SCHOOL_SCHOOL_CREATE`

**Request:**
```json
{
  "name": "Sunrise Public School",
  "code": "SPS",
  "type": "COMBINED",
  "addressLine1": "45 Oak Street",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "country": "IN",
  "phone": "+91-20-12345678",
  "email": "info@sunriseschool.edu"
}
```

**Response 201:** `SchoolResponse`

---

### GET /api/v1/school/schools/{id}

**Permission:** `SCHOOL_SCHOOL_READ`

**Response 200:** `SchoolResponse`

---

### GET /api/v1/school/schools

**Permission:** `SCHOOL_SCHOOL_READ`

**Query params:** `page`, `size`, `status`, `type`, `search`

**Response 200:** Paginated `SchoolSummaryResponse`

---

### PUT /api/v1/school/schools/{id}

**Permission:** `SCHOOL_SCHOOL_UPDATE`

**Response 200:** `SchoolResponse`

---

### PATCH /api/v1/school/schools/{id}/status

**Permission:** `SCHOOL_SCHOOL_UPDATE`

**Request:** `{ "status": "INACTIVE", "reason": "Annual maintenance" }`

**Response 200:** `SchoolResponse`

---

### DELETE /api/v1/school/schools/{id}

**Permission:** `SCHOOL_SCHOOL_DELETE`

**Response 204**
**Response 409:** Active students or teachers exist

---

## 11. Validation Rules

### CreateSchoolRequest
| Field        | Rule                                              |
|--------------|---------------------------------------------------|
| name         | Not blank, max 255, unique                        |
| code         | Optional; if provided: uppercase, max 10, unique  |
| type         | Not null, valid `SchoolType`                      |
| addressLine1 | Not blank, max 255                                |
| city         | Not blank, max 100                                |
| stateOrProvince | Not blank, max 100                             |
| country      | Not blank, ISO alpha-2, 2 chars                   |
| email        | Optional, valid email format                      |

---

## 12. Authorization

| Operation       | Permission              |
|-----------------|-------------------------|
| Create school   | `SCHOOL_SCHOOL_CREATE`  |
| Read school     | `SCHOOL_SCHOOL_READ`    |
| Update school   | `SCHOOL_SCHOOL_UPDATE`  |
| Change status   | `SCHOOL_SCHOOL_UPDATE`  |
| Delete school   | `SCHOOL_SCHOOL_DELETE`  |

---

## 13. Internal Service API

```
SchoolService.findById(Long id): SchoolResponse
SchoolService.findByIdOrThrow(Long id): SchoolResponse
SchoolService.existsById(Long id): boolean
SchoolService.isActive(Long id): boolean
```

Used by all other School modules to validate school existence and active status.

---

## 14. Integration Points

| Module           | Integration                                              |
|------------------|----------------------------------------------------------|
| `core-common`    | `SoftDeletableEntity`, exceptions, response envelope     |
| `core-file`      | Logo file upload and reference via `logo_file_id`        |
| `core-audit`     | Audit events for all CRUD and status changes             |
| All school modules | Every school-scoped entity holds `school_id` FK        |

---

## 15. Permission and Role Seeds

The following are seeded by the school module on startup:

**Roles:**
- `SCHOOL_ADMIN` — School administrator
- `SCHOOL_TEACHER` — Teacher
- `SCHOOL_STUDENT` — Student (optional login)

**School Permissions (partial — full list in `school-settings`):**
- `SCHOOL_SCHOOL_CREATE`, `SCHOOL_SCHOOL_READ`, `SCHOOL_SCHOOL_UPDATE`, `SCHOOL_SCHOOL_DELETE`

---

## 16. Flyway Migrations

```
V100__create_school_schools_table.sql
V101__seed_school_roles.sql
V102__seed_school_permissions.sql
V103__assign_school_permissions_to_school_admin.sql
```

---

## 17. Error Handling

| Scenario                            | Exception                      | HTTP |
|-------------------------------------|--------------------------------|------|
| School not found                    | `SchoolNotFoundException`      | 404  |
| Name already exists                 | `SchoolAlreadyExistsException` | 409  |
| Code already exists                 | `SchoolAlreadyExistsException` | 409  |
| Delete with active students/teachers| `ValidationException`          | 409  |
| Invalid status transition           | `ValidationException`          | 400  |

---

## 18. Logging and Audit

| Action                | Audit Code              | Details                |
|-----------------------|-------------------------|------------------------|
| School created        | `SCHOOL_CREATED`        | school_id, name        |
| School updated        | `SCHOOL_UPDATED`        | school_id              |
| School status changed | `SCHOOL_STATUS_CHANGED` | school_id, old, new    |
| School deleted        | `SCHOOL_DELETED`        | school_id              |

---

## 19. Testing Strategy

| Test Type       | Scope                                                        |
|-----------------|--------------------------------------------------------------|
| Unit — Service  | Create, find, update, status transitions, delete guard       |
| Repository test | Unique name/code constraint, status filter, soft-delete      |
| Controller test | All endpoints, HTTP codes, validation                        |
| Security tests  | `SCHOOL_SCHOOL_CREATE` required; 403 for teachers            |
| Integration     | Create school → set INACTIVE → attempt enrollment (rejected) |

---

## 20. Implementation Roadmap

### Task 1: Entity, migration, repository
- `School`, enums; `SchoolRepository`
- Flyway: `V100__create_school_schools_table.sql`

### Task 2: Service CRUD
- `createSchool`, `findById`, `listSchools`, `updateSchool`, `changeStatus`, `deleteSchool`

### Task 3: Role and permission seeding
- Flyway: `V101`–`V103`

### Task 4: Controller and DTOs

### Task 5: Audit integration

---

## 21. Acceptance Criteria

- [ ] School code is immutable after creation
- [ ] Duplicate school name returns 409
- [ ] An INACTIVE school's `isActive()` returns false — used by all other school modules
- [ ] A school with active students cannot be deleted
- [ ] Logo upload links `logo_file_id` to a valid file record
- [ ] All CRUD and status changes produce audit log entries

---

## 22. Out of Scope

- Multi-campus / branch management
- School fee structures, HR, payroll
- External directory service (LDAP) integration
