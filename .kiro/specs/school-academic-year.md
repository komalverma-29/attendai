# Specification: school-academic-year

## 1. Overview

`school-academic-year` manages the academic year lifecycle for a school. An academic year defines the time boundary within which all school operations — class assignments, student section enrollments, timetables, attendance, and leave — are scoped.

Every attendance record, section enrollment, and timetable entry in the system is anchored to an academic year. This makes it possible to query historical data across years without ambiguity.

---

## 2. Scope and Objectives

**In scope:**
- Academic year creation and management for a school
- Academic year status lifecycle (UPCOMING, ACTIVE, COMPLETED, CANCELLED)
- Ensuring only one academic year is ACTIVE per school at any time
- Academic year date range validation
- Promoting an UPCOMING year to ACTIVE (year rollover)
- Completing an ACTIVE year

**Out of scope:**
- Academic calendar (holidays and working days — belongs in `school-academic-calendar`)
- Term or semester sub-divisions within an academic year (V1: single period per year)
- Class promotion (automatic promotion of students to the next class at year end)

---

## 3. Functional Requirements

### FR-AY-01: Create Academic Year
Create a new academic year for a school with a name, start date, and end date. An academic year is created in `UPCOMING` status.

### FR-AY-02: Get Academic Year by ID
Retrieve a single academic year for a school.

### FR-AY-03: Get Active Academic Year for School
Retrieve the currently `ACTIVE` academic year for a school. Used extensively by attendance processing, section enrollment, and timetable modules.

### FR-AY-04: List Academic Years for School
Return a paginated list of academic years for a school, ordered by start date descending.

### FR-AY-05: Update Academic Year
Update name and date range while the year is in `UPCOMING` status. Updates to an `ACTIVE` year are restricted to name only.

### FR-AY-06: Activate Academic Year
Transition an `UPCOMING` academic year to `ACTIVE`. Only one year can be `ACTIVE` per school. Activating a year while another is active is rejected unless the current active year is completed first.

### FR-AY-07: Complete Academic Year
Transition the `ACTIVE` academic year to `COMPLETED`. This marks the year as finished. Attendance can still be queried but no new attendance can be recorded for this year.

### FR-AY-08: Cancel Academic Year
Transition an `UPCOMING` academic year to `CANCELLED`. Cannot cancel an `ACTIVE` or `COMPLETED` year.

### FR-AY-09: Delete Academic Year (Soft)
Soft-delete an academic year. Only `CANCELLED` or `UPCOMING` years with no section enrollments can be deleted.

---

## 4. Non-Functional Requirements

- `getActiveAcademicYear(schoolId)` is called on every attendance processing operation. It must be cached or indexed for fast retrieval.
- Only one `ACTIVE` academic year per school is enforced at the database level via a partial unique index.
- Academic year date ranges must not overlap with other non-cancelled years within the same school.

---

## 5. Business Rules

- BR-AY-01: A school can have at most one `ACTIVE` academic year at a time.
- BR-AY-02: Academic year start date must be before end date.
- BR-AY-03: Academic year date ranges must not overlap with other non-`CANCELLED` years for the same school.
- BR-AY-04: An `ACTIVE` year's date range cannot be modified. Only the name can change.
- BR-AY-05: A `COMPLETED` year is immutable — no fields can be changed.
- BR-AY-06: Attendance processing is only permitted for the `ACTIVE` academic year.
- BR-AY-07: A `CANCELLED` or `UPCOMING` year with no dependencies (sections, enrollments) can be soft-deleted.

---

## 6. Academic Year Status State Machine

```
[Created] → UPCOMING
UPCOMING  → ACTIVE      (activate — no other active year exists)
UPCOMING  → CANCELLED   (cancel)
ACTIVE    → COMPLETED   (complete year)
COMPLETED → [immutable]
CANCELLED → [immutable, soft-deletable]
```

---

## 7. Domain Model

### SchoolAcademicYear Entity

| Field       | Type               | Description                                            |
|-------------|--------------------|--------------------------------------------------------|
| id          | Long               | Surrogate PK                                           |
| schoolId    | Long               | FK → school_schools(id), NOT NULL                      |
| name        | String             | e.g. "2025–2026", NOT NULL, max 100                    |
| startDate   | LocalDate          | NOT NULL                                               |
| endDate     | LocalDate          | NOT NULL, must be after startDate                      |
| status      | AcademicYearStatus | Enum: UPCOMING, ACTIVE, COMPLETED, CANCELLED           |
| description | String             | Optional, max 500                                      |
| isDeleted   | boolean            | Soft delete flag                                       |
| deletedAt   | LocalDateTime      | Soft delete timestamp                                  |
| createdAt   | LocalDateTime      | Audit                                                  |
| updatedAt   | LocalDateTime      | Audit                                                  |
| createdBy   | Long               | Audit                                                  |
| updatedBy   | Long               | Audit                                                  |

### AcademicYearStatus Enum
- `UPCOMING`
- `ACTIVE`
- `COMPLETED`
- `CANCELLED`

---

## 8. Data Model

### Table: `school_academic_years`

```sql
CREATE TABLE school_academic_years (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(100)     NOT NULL,
    start_date  DATE             NOT NULL,
    end_date    DATE             NOT NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'UPCOMING',
    description VARCHAR(500)     NULL,
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_academic_years_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    INDEX idx_school_academic_years_school (school_id),
    INDEX idx_school_academic_years_status (school_id, status),
    UNIQUE uq_school_academic_year_name (school_id, name)
);
```

The "one active year per school" constraint is enforced in service logic (not a DB unique index, since MariaDB partial unique indexes require special handling). The service validates this before every activation.

---

## 9. Package Organization

```
com.attendai.school.academicyear
├── entity
│   ├── SchoolAcademicYear.java
│   └── AcademicYearStatus.java
├── repository
│   └── SchoolAcademicYearRepository.java
├── service
│   ├── AcademicYearService.java
│   └── AcademicYearServiceImpl.java
├── controller
│   └── AcademicYearController.java
├── dto
│   ├── CreateAcademicYearRequest.java
│   ├── UpdateAcademicYearRequest.java
│   ├── AcademicYearResponse.java
│   └── AcademicYearSummaryResponse.java
├── mapper
│   └── AcademicYearMapper.java
└── exception
    ├── AcademicYearNotFoundException.java
    └── ActiveAcademicYearAlreadyExistsException.java
```

---

## 10. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/academic-years`

### POST — Create Academic Year

**Permission:** `SCHOOL_ACADEMIC_YEAR_CREATE`

**Request:**
```json
{
  "name": "2025-2026",
  "startDate": "2025-06-01",
  "endDate": "2026-03-31",
  "description": "Standard academic year"
}
```

**Response 201:** `AcademicYearResponse`

---

### GET /active — Get Active Academic Year

**Permission:** `SCHOOL_ACADEMIC_YEAR_READ`

**Response 200:** `AcademicYearResponse`
**Response 404:** No active academic year for this school

---

### GET /{id}

**Permission:** `SCHOOL_ACADEMIC_YEAR_READ`

**Response 200:** `AcademicYearResponse`

---

### GET — List Academic Years

**Permission:** `SCHOOL_ACADEMIC_YEAR_READ`

**Query params:** `page`, `size`, `status`

**Response 200:** Paginated `AcademicYearSummaryResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_ACADEMIC_YEAR_UPDATE`

**Note:** Updates to date range are only allowed for `UPCOMING` years.

**Response 200:** `AcademicYearResponse`

---

### PATCH /{id}/activate

**Permission:** `SCHOOL_ACADEMIC_YEAR_UPDATE`

**Response 200:** `AcademicYearResponse`
**Response 409:** Another academic year is already ACTIVE

---

### PATCH /{id}/complete

**Permission:** `SCHOOL_ACADEMIC_YEAR_UPDATE`

**Response 200:** `AcademicYearResponse`

---

### PATCH /{id}/cancel

**Permission:** `SCHOOL_ACADEMIC_YEAR_UPDATE`

**Response 200:** `AcademicYearResponse`

---

### DELETE /{id}

**Permission:** `SCHOOL_ACADEMIC_YEAR_DELETE`

**Response 204**

---

## 11. Validation Rules

### CreateAcademicYearRequest
| Field     | Rule                                                          |
|-----------|---------------------------------------------------------------|
| name      | Not blank, max 100, unique within school                      |
| startDate | Not null, must be a valid date                                |
| endDate   | Not null, must be after startDate                             |

---

## 12. Authorization

| Operation              | Permission                        |
|------------------------|-----------------------------------|
| Create                 | `SCHOOL_ACADEMIC_YEAR_CREATE`     |
| Read / list            | `SCHOOL_ACADEMIC_YEAR_READ`       |
| Update                 | `SCHOOL_ACADEMIC_YEAR_UPDATE`     |
| Activate/complete/cancel| `SCHOOL_ACADEMIC_YEAR_UPDATE`    |
| Delete                 | `SCHOOL_ACADEMIC_YEAR_DELETE`     |

---

## 13. Internal Service API

```
AcademicYearService.getActiveAcademicYear(Long schoolId): AcademicYearResponse
AcademicYearService.getActiveAcademicYearOrThrow(Long schoolId): AcademicYearResponse
AcademicYearService.existsById(Long id): boolean
AcademicYearService.isActive(Long id): boolean
AcademicYearService.isDateWithinAcademicYear(Long academicYearId, LocalDate date): boolean
```

`getActiveAcademicYear` is the most frequently called method in the entire school module — invoked on every attendance event processing cycle. It should be cached in the `SchoolSettings` cache or via `@Cacheable` with school-scoped eviction.

---

## 14. Integration Points

| Module                    | Integration                                              |
|---------------------------|----------------------------------------------------------|
| `school-school`           | `SchoolService.isActive()` before create                 |
| `school-academic-calendar`| Calendar is scoped to an academic year                   |
| `school-section`          | Section enrollments are scoped to academic year          |
| `school-timetable`        | Timetables are scoped to academic year                   |
| `school-daily-attendance` | `getActiveAcademicYearOrThrow()` on every event process  |
| `school-attendance-rules` | Rules are scoped to academic year                        |
| `core-audit`              | All state transitions are audit-logged                   |

---

## 15. Error Handling

| Scenario                            | Exception                               | HTTP |
|-------------------------------------|-----------------------------------------|------|
| Academic year not found             | `AcademicYearNotFoundException`         | 404  |
| Activating when one already active  | `ActiveAcademicYearAlreadyExistsException` | 409 |
| Date range overlap                  | `ValidationException`                   | 409  |
| Modifying COMPLETED year            | `ValidationException`                   | 400  |
| Cancelling ACTIVE year              | `ValidationException`                   | 400  |

---

## 16. Logging and Audit

| Action                     | Audit Code                       | Details                    |
|----------------------------|----------------------------------|----------------------------|
| Academic year created      | `ACADEMIC_YEAR_CREATED`          | year_id, school_id         |
| Academic year updated      | `ACADEMIC_YEAR_UPDATED`          | year_id                    |
| Academic year activated    | `ACADEMIC_YEAR_ACTIVATED`        | year_id                    |
| Academic year completed    | `ACADEMIC_YEAR_COMPLETED`        | year_id                    |
| Academic year cancelled    | `ACADEMIC_YEAR_CANCELLED`        | year_id                    |

---

## 17. Flyway Migrations

```
V107__create_school_academic_years_table.sql
```

---

## 18. Testing Strategy

| Test Type       | Scope                                                             |
|-----------------|-------------------------------------------------------------------|
| Unit — Service  | Create, activate (with and without existing active year)          |
| Unit — Service  | Date overlap validation, complete, cancel                         |
| Unit — Service  | `isDateWithinAcademicYear` boundary values                        |
| Repository test | `findActiveBySchoolId`, status filter, overlap check query        |
| Controller test | All endpoints, status transition HTTP codes                       |
| Integration     | Activate year → trigger attendance → complete year → verify block |

---

## 19. Acceptance Criteria

- [ ] Only one ACTIVE academic year exists per school at any time
- [ ] Creating a year with overlapping dates returns 409
- [ ] Activating when another year is already ACTIVE returns 409
- [ ] A COMPLETED year's fields are immutable
- [ ] `getActiveAcademicYearOrThrow` throws 404 when no active year exists
- [ ] `isDateWithinAcademicYear` correctly validates boundary dates (inclusive)
- [ ] All status transitions are audit-logged

---

## 20. Out of Scope

- Semester or term sub-divisions
- Academic calendar (school-academic-calendar)
- Automatic student promotion on year completion
- Academic year archival / export
