# Specification: school-teacher

## 1. Overview

`school-teacher` manages teacher profiles within a school. A teacher is a school-domain entity that links a Core `Person` to an optional Core `User` within the context of a specific school. Teachers may or may not have platform login access depending on school configuration.

Teacher profiles are the basis for teacher-section-subject assignments and timetable scheduling in subsequent modules.

---

## 2. Scope and Objectives

**In scope:**
- Teacher profile creation and management
- Linking a teacher to a Core `Person` (required) and optionally a Core `User`
- Teacher status management (ACTIVE, INACTIVE, ON_LEAVE)
- Employee code tracking (school-internal identifier)
- Teacher qualification and department metadata
- Face profile enrollment initiation (delegates to `core-face`)
- Listing and searching teachers within a school
- Teacher soft delete

**Out of scope:**
- Teacher payroll, appraisals, or HR workflows
- Teacher-subject assignments (belongs in `school-teacher-assignment`)
- Timetable scheduling (belongs in `school-timetable`)
- Leave management (belongs in `school-leave`)

---

## 3. Functional Requirements

### FR-TEACHER-01: Create Teacher Profile
Create a teacher record linking a `Person` to a school. Optionally link a `User` account. Set an employee code.

### FR-TEACHER-02: Get Teacher by ID
Retrieve a single non-deleted teacher within a school.

### FR-TEACHER-03: List Teachers for School
Return a paginated list of teachers in a school. Filterable by status and searchable by name.

### FR-TEACHER-04: Update Teacher Profile
Update teacher-specific fields: employee code, qualification, department, designation, notes.

### FR-TEACHER-05: Change Teacher Status
Transition between ACTIVE, INACTIVE, ON_LEAVE.

### FR-TEACHER-06: Assign User Account to Teacher
Link a Core `User` to an existing teacher record (for teachers who gain login access after initial enrollment).

### FR-TEACHER-07: Remove User Account from Teacher
Unlink the Core `User` from a teacher. Revokes the `SCHOOL_TEACHER` role.

### FR-TEACHER-08: Delete Teacher (Soft)
Soft-delete a teacher. Rejected if the teacher has active section assignments in the current academic year.

---

## 4. Business Rules

- BR-TEACHER-01: A `Person` can be a teacher in at most one school at a time.
- BR-TEACHER-02: Employee code must be unique within a school.
- BR-TEACHER-03: If a `User` is linked, the `User.personId` must match the teacher's `personId`.
- BR-TEACHER-04: Linking a `User` account automatically assigns the `SCHOOL_TEACHER` role.
- BR-TEACHER-05: Unlinking or deleting a teacher automatically revokes the `SCHOOL_TEACHER` role.
- BR-TEACHER-06: An `INACTIVE` teacher cannot be assigned to new sections or timetable slots.
- BR-TEACHER-07: A teacher with active timetable assignments cannot be soft-deleted.

---

## 5. Domain Model

### SchoolTeacher Entity

| Field         | Type          | Description                                             |
|---------------|---------------|---------------------------------------------------------|
| id            | Long          | Surrogate PK                                            |
| schoolId      | Long          | FK → school_schools(id), NOT NULL                       |
| personId      | Long          | FK → persons(id) [Core], NOT NULL                       |
| userId        | Long          | FK → users(id) [Core], nullable                         |
| employeeCode  | String        | School-internal unique code, max 50                     |
| designation   | String        | Optional, e.g. "Science Teacher", max 100               |
| qualification | String        | Optional, max 255                                       |
| department    | String        | Optional, max 100                                       |
| status        | TeacherStatus | Enum: ACTIVE, INACTIVE, ON_LEAVE                        |
| notes         | String        | Optional, max 500                                       |
| joiningDate   | LocalDate     | Date of joining the school, nullable                    |
| isDeleted     | boolean       | Soft delete flag                                        |
| deletedAt     | LocalDateTime | Soft delete timestamp                                   |
| createdAt     | LocalDateTime | Audit                                                   |
| updatedAt     | LocalDateTime | Audit                                                   |
| createdBy     | Long          | Audit                                                   |
| updatedBy     | Long          | Audit                                                   |

### TeacherStatus Enum
- `ACTIVE` — available for assignments
- `INACTIVE` — not available
- `ON_LEAVE` — temporarily unavailable

---

## 6. Data Model

### Table: `school_teachers`

```sql
CREATE TABLE school_teachers (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id     BIGINT UNSIGNED  NOT NULL,
    person_id     BIGINT UNSIGNED  NOT NULL,
    user_id       BIGINT UNSIGNED  NULL,
    employee_code VARCHAR(50)      NULL,
    designation   VARCHAR(100)     NULL,
    qualification VARCHAR(255)     NULL,
    department    VARCHAR(100)     NULL,
    status        VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes         VARCHAR(500)     NULL,
    joining_date  DATE             NULL,
    is_deleted    BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME         NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    BIGINT UNSIGNED  NULL,
    updated_by    BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_teachers_school  FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_teachers_person_school (person_id, school_id),
    UNIQUE uq_school_teachers_employee_code (school_id, employee_code),
    UNIQUE uq_school_teachers_user (user_id),
    INDEX idx_school_teachers_school_id (school_id),
    INDEX idx_school_teachers_status (status)
);
```

---

## 7. Package Organization

```
com.attendai.school.teacher
├── entity
│   ├── SchoolTeacher.java
│   └── TeacherStatus.java
├── repository
│   └── SchoolTeacherRepository.java
├── service
│   ├── SchoolTeacherService.java
│   └── SchoolTeacherServiceImpl.java
├── controller
│   └── SchoolTeacherController.java
├── dto
│   ├── CreateTeacherRequest.java
│   ├── UpdateTeacherRequest.java
│   ├── AssignUserToTeacherRequest.java
│   ├── TeacherResponse.java
│   └── TeacherSummaryResponse.java
├── mapper
│   └── SchoolTeacherMapper.java
└── exception
    └── TeacherNotFoundException.java
```

---

## 8. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/teachers`

### POST — Create Teacher

**Permission:** `SCHOOL_TEACHER_CREATE`

**Request:**
```json
{
  "personId": 102,
  "userId": null,
  "employeeCode": "TCH-001",
  "designation": "Mathematics Teacher",
  "qualification": "B.Ed, M.Sc Mathematics",
  "department": "Science & Maths",
  "joiningDate": "2023-06-01"
}
```

**Response 201:** `TeacherResponse`

---

### GET /{id}

**Permission:** `SCHOOL_TEACHER_READ`

**Response 200:** `TeacherResponse`

---

### GET — List Teachers

**Permission:** `SCHOOL_TEACHER_READ`

**Query params:** `page`, `size`, `status`, `search` (name, employee code)

**Response 200:** Paginated `TeacherSummaryResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_TEACHER_UPDATE`

**Response 200:** `TeacherResponse`

---

### PATCH /{id}/status

**Permission:** `SCHOOL_TEACHER_UPDATE`

**Request:** `{ "status": "ON_LEAVE" }`

**Response 200:** `TeacherResponse`

---

### POST /{id}/assign-user

**Permission:** `SCHOOL_TEACHER_UPDATE`

**Request:** `{ "userId": 15 }`

**Response 200:** `TeacherResponse`

---

### DELETE /{id}/remove-user

**Permission:** `SCHOOL_TEACHER_UPDATE`

**Response 200:** `TeacherResponse`

---

### DELETE /{id}

**Permission:** `SCHOOL_TEACHER_DELETE`

**Response 204**
**Response 409:** Active timetable assignments exist

---

## 9. Validation Rules

### CreateTeacherRequest
| Field        | Rule                                                          |
|--------------|---------------------------------------------------------------|
| personId     | Not null, must reference non-deleted Core Person              |
| userId       | Optional; if provided, must reference active Core User        |
| employeeCode | Optional; unique within school, max 50                        |
| designation  | Optional, max 100                                             |
| joiningDate  | Optional, must be past or today                               |

---

## 10. Authorization

| Operation           | Permission               |
|---------------------|--------------------------|
| Create teacher      | `SCHOOL_TEACHER_CREATE`  |
| Read teacher        | `SCHOOL_TEACHER_READ`    |
| Update teacher      | `SCHOOL_TEACHER_UPDATE`  |
| Change status       | `SCHOOL_TEACHER_UPDATE`  |
| Assign/remove user  | `SCHOOL_TEACHER_UPDATE`  |
| Delete teacher      | `SCHOOL_TEACHER_DELETE`  |

---

## 11. Integration Points

| Module                | Integration                                                    |
|-----------------------|----------------------------------------------------------------|
| `core-person`         | `PersonService.existsById()` on create                         |
| `core-user`           | Validates userId exists and is ACTIVE                          |
| `core-role`           | Assign/revoke `SCHOOL_TEACHER` role on user link/unlink        |
| `school-school`       | `SchoolService.isActive()` validated before create             |
| `school-teacher-assignment` | Guards delete if active assignments exist              |
| `core-audit`          | Audit events for all write operations                          |

---

## 12. Error Handling

| Scenario                             | Exception                        | HTTP |
|--------------------------------------|----------------------------------|------|
| Teacher not found                    | `TeacherNotFoundException`       | 404  |
| Person already teacher in school     | `ResourceAlreadyExistsException` | 409  |
| Employee code duplicate in school    | `ResourceAlreadyExistsException` | 409  |
| Delete with active assignments       | `ValidationException`            | 409  |
| Person-User mismatch                 | `ValidationException`            | 400  |

---

## 13. Flyway Migrations

```
V105__create_school_teachers_table.sql
```

---

## 14. Acceptance Criteria

- [ ] Creating a teacher with a linked user assigns `SCHOOL_TEACHER` role
- [ ] Removing user from teacher revokes `SCHOOL_TEACHER` role
- [ ] A person cannot be a teacher in more than one school simultaneously
- [ ] Employee code is unique within a school
- [ ] Teacher with active timetable assignments cannot be deleted
- [ ] All write operations produce audit log entries

---

## 15. Out of Scope

- Teacher payroll, appraisals, performance reviews
- Teacher timetable (school-timetable)
- Teacher leave (school-leave)
