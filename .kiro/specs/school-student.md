# Specification: school-student

## 1. Overview

`school-student` manages student enrollment and profiles within a school. A student is a school-domain entity that links a Core `Person` to a school with a school-assigned roll number and enrollment metadata.

Student login is optional and controlled by school settings. If login is enabled for a student, a Core `User` is linked, and the `SCHOOL_STUDENT` role is assigned.

Students are enrolled into sections (via `school-section`) and form the primary subjects of attendance tracking.

---

## 2. Scope and Objectives

**In scope:**
- Student enrollment (creation) within a school
- Linking student to Core `Person` (required) and optionally Core `User`
- Roll number assignment (unique within a section)
- Admission number (school-wide unique identifier)
- Student status management (ACTIVE, INACTIVE, TRANSFERRED, GRADUATED)
- Section assignment tracking (which section the student currently belongs to)
- Academic year enrollment tracking
- Student face profile enrollment initiation (delegates to `core-face`)
- Soft delete

**Out of scope:**
- Student fees, exam results, library records
- Parent/guardian portal
- Detailed academic transcripts
- Transfer certificate generation

---

## 3. Functional Requirements

### FR-STU-01: Enroll Student
Create a student record by linking a `Person` to a school, assigning an admission number, and optionally a user account. The student is initially not assigned to any section.

### FR-STU-02: Get Student by ID
Retrieve a single non-deleted student within a school.

### FR-STU-03: Get Student by Admission Number
Retrieve a student by the school-assigned admission number.

### FR-STU-04: List Students for School
Return a paginated list of students. Filterable by status, section, academic year. Searchable by name, admission number, roll number.

### FR-STU-05: Update Student Profile
Update student-specific fields: admission number, guardian name, guardian phone, blood group, notes.

### FR-STU-06: Change Student Status
Transition: ACTIVE → INACTIVE, ACTIVE → TRANSFERRED, ACTIVE → GRADUATED.

### FR-STU-07: Assign User Account to Student
Link a Core `User` to a student (when student login is enabled). Assigns `SCHOOL_STUDENT` role.

### FR-STU-08: Remove User Account from Student
Unlink the Core `User`. Revokes `SCHOOL_STUDENT` role.

### FR-STU-09: Delete Student (Soft)
Soft-delete a student record. Rejected if the student has attendance records in the current academic year.

### FR-STU-10: Get Students for Section
Return all active students enrolled in a specific section for a given academic year.

---

## 4. Business Rules

- BR-STU-01: A `Person` can be enrolled in at most one school at a time as a student.
- BR-STU-02: Admission number must be unique within a school.
- BR-STU-03: Roll number must be unique within a section and academic year combination.
- BR-STU-04: If a `User` is linked, `user.personId` must match `student.personId`.
- BR-STU-05: An `INACTIVE`, `TRANSFERRED`, or `GRADUATED` student cannot have new attendance recorded.
- BR-STU-06: A student with attendance records in the current academic year cannot be soft-deleted.
- BR-STU-07: Student section assignment for a given academic year is managed by `school-section`.
- BR-STU-08: `GRADUATED` and `TRANSFERRED` statuses are terminal — they cannot transition back to `ACTIVE` without explicit re-enrollment.

---

## 5. Student Status State Machine

```
[Enrolled] → ACTIVE
ACTIVE     → INACTIVE      (admin deactivates)
ACTIVE     → TRANSFERRED   (student moves to another school)
ACTIVE     → GRADUATED     (academic year completion)
INACTIVE   → ACTIVE        (admin re-activates)
TRANSFERRED → [terminal]
GRADUATED   → [terminal]
Any        → [Deleted]     (soft delete, if no current-year attendance)
```

---

## 6. Domain Model

### SchoolStudent Entity

| Field            | Type          | Description                                               |
|------------------|---------------|-----------------------------------------------------------|
| id               | Long          | Surrogate PK                                              |
| schoolId         | Long          | FK → school_schools(id), NOT NULL                         |
| personId         | Long          | FK → persons(id) [Core], NOT NULL                         |
| userId           | Long          | FK → users(id) [Core], nullable                           |
| admissionNumber  | String        | Unique within school, max 50, NOT NULL                    |
| status           | StudentStatus | Enum: ACTIVE, INACTIVE, TRANSFERRED, GRADUATED            |
| bloodGroup       | String        | Optional, max 5 (e.g. "O+")                               |
| guardianName     | String        | Optional, max 200                                         |
| guardianPhone    | String        | Optional, max 30                                          |
| guardianEmail    | String        | Optional, max 255                                         |
| enrollmentDate   | LocalDate     | Date of enrollment, NOT NULL                              |
| notes            | String        | Optional, max 500                                         |
| isDeleted        | boolean       | Soft delete flag                                          |
| deletedAt        | LocalDateTime | Soft delete timestamp                                     |
| createdAt        | LocalDateTime | Audit                                                     |
| updatedAt        | LocalDateTime | Audit                                                     |
| createdBy        | Long          | Audit                                                     |
| updatedBy        | Long          | Audit                                                     |

### StudentStatus Enum
- `ACTIVE`
- `INACTIVE`
- `TRANSFERRED`
- `GRADUATED`

---

## 7. Data Model

### Table: `school_students`

```sql
CREATE TABLE school_students (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    person_id        BIGINT UNSIGNED  NOT NULL,
    user_id          BIGINT UNSIGNED  NULL,
    admission_number VARCHAR(50)      NOT NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    blood_group      VARCHAR(5)       NULL,
    guardian_name    VARCHAR(200)     NULL,
    guardian_phone   VARCHAR(30)      NULL,
    guardian_email   VARCHAR(255)     NULL,
    enrollment_date  DATE             NOT NULL,
    notes            VARCHAR(500)     NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_students_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_students_person_school (person_id, school_id),
    UNIQUE uq_school_students_admission (school_id, admission_number),
    UNIQUE uq_school_students_user (user_id),
    INDEX idx_school_students_school_id (school_id),
    INDEX idx_school_students_status (status)
);
```

---

## 8. Package Organization

```
com.attendai.school.student
├── entity
│   ├── SchoolStudent.java
│   └── StudentStatus.java
├── repository
│   └── SchoolStudentRepository.java
├── service
│   ├── SchoolStudentService.java
│   └── SchoolStudentServiceImpl.java
├── controller
│   └── SchoolStudentController.java
├── dto
│   ├── EnrollStudentRequest.java
│   ├── UpdateStudentRequest.java
│   ├── AssignUserToStudentRequest.java
│   ├── StudentResponse.java
│   └── StudentSummaryResponse.java
├── mapper
│   └── SchoolStudentMapper.java
└── exception
    └── StudentNotFoundException.java
```

---

## 9. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/students`

### POST — Enroll Student

**Permission:** `SCHOOL_STUDENT_CREATE`

**Request:**
```json
{
  "personId": 103,
  "admissionNumber": "ADM-2025-001",
  "enrollmentDate": "2025-06-01",
  "bloodGroup": "B+",
  "guardianName": "Robert Doe",
  "guardianPhone": "+91-9876500001"
}
```

**Response 201:** `StudentResponse`

---

### GET /{id}

**Permission:** `SCHOOL_STUDENT_READ`

**Response 200:** `StudentResponse`

---

### GET — List Students

**Permission:** `SCHOOL_STUDENT_READ`

**Query params:** `page`, `size`, `status`, `sectionId`, `academicYearId`, `search`

**Response 200:** Paginated `StudentSummaryResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_STUDENT_UPDATE`

**Response 200:** `StudentResponse`

---

### PATCH /{id}/status

**Permission:** `SCHOOL_STUDENT_UPDATE`

**Request:** `{ "status": "GRADUATED", "reason": "Passed Grade 12" }`

**Response 200:** `StudentResponse`

---

### POST /{id}/assign-user

**Permission:** `SCHOOL_STUDENT_UPDATE`

**Request:** `{ "userId": 20 }`

**Response 200:** `StudentResponse`

---

### DELETE /{id}/remove-user

**Permission:** `SCHOOL_STUDENT_UPDATE`

**Response 200:** `StudentResponse`

---

### DELETE /{id}

**Permission:** `SCHOOL_STUDENT_DELETE`

**Response 204**
**Response 409:** Student has attendance records in current academic year

---

## 10. Validation Rules

### EnrollStudentRequest
| Field           | Rule                                                      |
|-----------------|-----------------------------------------------------------|
| personId        | Not null, non-deleted Core Person                         |
| admissionNumber | Not blank, unique within school, max 50                   |
| enrollmentDate  | Not null, not future                                      |
| bloodGroup      | Optional, max 5                                           |
| guardianPhone   | Optional, max 30                                          |
| guardianEmail   | Optional, valid email format                              |

---

## 11. Authorization

| Operation            | Permission                |
|----------------------|---------------------------|
| Enroll student       | `SCHOOL_STUDENT_CREATE`   |
| Read student         | `SCHOOL_STUDENT_READ`     |
| Update student       | `SCHOOL_STUDENT_UPDATE`   |
| Change status        | `SCHOOL_STUDENT_UPDATE`   |
| Assign/remove user   | `SCHOOL_STUDENT_UPDATE`   |
| Delete student       | `SCHOOL_STUDENT_DELETE`   |

---

## 12. Internal Service API

```
SchoolStudentService.existsById(Long studentId): boolean
SchoolStudentService.findById(Long studentId): StudentResponse
SchoolStudentService.findByPersonId(Long personId): Optional<StudentResponse>
SchoolStudentService.isActive(Long studentId): boolean
```

Used by `school-daily-attendance` and `school-section` to validate student existence and active status.

---

## 13. Integration Points

| Module                  | Integration                                                  |
|-------------------------|--------------------------------------------------------------|
| `core-person`           | `PersonService.existsById()` on enroll                       |
| `core-role`             | Assign/revoke `SCHOOL_STUDENT` role on user link/unlink      |
| `school-school`         | `SchoolService.isActive()` validated before enroll           |
| `school-section`        | Student-section enrollment managed in school-section         |
| `school-daily-attendance`| Guards delete if current-year attendance exists             |
| `core-audit`            | Audit events for all write operations                        |

---

## 14. Error Handling

| Scenario                                | Exception                        | HTTP |
|-----------------------------------------|----------------------------------|------|
| Student not found                       | `StudentNotFoundException`       | 404  |
| Person already enrolled in school       | `ResourceAlreadyExistsException` | 409  |
| Duplicate admission number in school    | `ResourceAlreadyExistsException` | 409  |
| Delete with current-year attendance     | `ValidationException`            | 409  |
| Terminal status re-activation           | `ValidationException`            | 400  |

---

## 15. Flyway Migrations

```
V106__create_school_students_table.sql
```

---

## 16. Acceptance Criteria

- [ ] A person cannot be enrolled as student in more than one school simultaneously
- [ ] Admission number is unique within a school
- [ ] `TRANSFERRED` and `GRADUATED` students cannot transition back to `ACTIVE` without re-enrollment
- [ ] A student with current-year attendance records cannot be soft-deleted
- [ ] Linking a user account assigns `SCHOOL_STUDENT` role
- [ ] `isActive()` returns false for INACTIVE, TRANSFERRED, GRADUATED students

---

## 17. Out of Scope

- Student fees, exams, report cards
- Parent/guardian portal access
- Bulk import of student records (future)
- Transfer certificate document generation
