# Specification: school-teacher-assignment

## 1. Overview

`school-teacher-assignment` manages the assignment of teachers to subject-section combinations within an academic year. A teacher assignment defines: which teacher teaches which subject in which section for a given academic year.

These assignments are the basis for timetable scheduling and for subject-level attendance tracking (if applicable in future). In V1, they primarily feed the timetable module and provide the data for "which teacher is responsible for which section's subject".

---

## 2. Scope and Objectives

**In scope:**
- Creating teacher-subject-section assignments for an academic year
- Listing assignments by teacher, section, or subject
- Updating an assignment (change the teacher for a subject-section)
- Deactivating and deleting assignments
- Class teacher designation (one teacher is designated as class teacher for a section)

**Out of scope:**
- Timetable period scheduling (belongs in `school-timetable`)
- Teacher availability/workload management
- Substitute teacher assignment

---

## 3. Functional Requirements

### FR-ASSIGN-01: Create Assignment
Assign a teacher to a subject within a section for an academic year.

### FR-ASSIGN-02: Get Assignment by ID
Retrieve a single assignment.

### FR-ASSIGN-03: List Assignments for Section
Return all subject-teacher assignments for a given section and academic year.

### FR-ASSIGN-04: List Assignments for Teacher
Return all section-subject assignments held by a teacher in an academic year.

### FR-ASSIGN-05: Update Assignment
Change the teacher for an existing subject-section assignment.

### FR-ASSIGN-06: Deactivate Assignment
Mark an assignment as inactive (e.g., teacher leaves mid-year).

### FR-ASSIGN-07: Delete Assignment (Soft)
Soft-delete an assignment. Rejected if timetable entries reference it.

### FR-ASSIGN-08: Designate Class Teacher
Mark a specific teacher assignment as the class teacher for a section. Only one class teacher per section per academic year.

---

## 4. Business Rules

- BR-ASSIGN-01: A subject can have at most one active teacher assignment per section per academic year.
- BR-ASSIGN-02: A teacher must be `ACTIVE` to be assigned.
- BR-ASSIGN-03: The subject must be associated with the class (via `school-class-subjects`) that contains the section.
- BR-ASSIGN-04: The section must be `ACTIVE`.
- BR-ASSIGN-05: Only one class teacher is permitted per section per academic year.
- BR-ASSIGN-06: An `INACTIVE` assignment cannot feed the timetable.

---

## 5. Domain Model

### TeacherAssignment Entity

| Field          | Type                   | Description                                            |
|----------------|------------------------|--------------------------------------------------------|
| id             | Long                   | Surrogate PK                                           |
| schoolId       | Long                   | FK → school_schools(id), NOT NULL                      |
| academicYearId | Long                   | FK → school_academic_years(id), NOT NULL               |
| sectionId      | Long                   | FK → school_sections(id), NOT NULL                     |
| subjectId      | Long                   | FK → school_subjects(id), NOT NULL                     |
| teacherId      | Long                   | FK → school_teachers(id), NOT NULL                     |
| isClassTeacher | boolean                | True if this teacher is designated class teacher        |
| status         | AssignmentStatus       | Enum: ACTIVE, INACTIVE                                 |
| notes          | String                 | Optional, max 500                                      |
| isDeleted      | boolean                | Soft delete flag                                       |
| deletedAt      | LocalDateTime          | Soft delete timestamp                                  |
| createdAt      | LocalDateTime          | Audit                                                  |
| updatedAt      | LocalDateTime          | Audit                                                  |
| createdBy      | Long                   | Audit                                                  |
| updatedBy      | Long                   | Audit                                                  |

---

## 6. Data Model

### Table: `school_teacher_assignments`

```sql
CREATE TABLE school_teacher_assignments (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    subject_id       BIGINT UNSIGNED  NOT NULL,
    teacher_id       BIGINT UNSIGNED  NOT NULL,
    is_class_teacher BOOLEAN          NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes            VARCHAR(500)     NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_ta_school  FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_ta_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_ta_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_ta_subject FOREIGN KEY (subject_id)       REFERENCES school_subjects(id),
    CONSTRAINT fk_ta_teacher FOREIGN KEY (teacher_id)       REFERENCES school_teachers(id),
    UNIQUE uq_teacher_assignments (section_id, subject_id, academic_year_id),
    INDEX idx_ta_section_year (section_id, academic_year_id),
    INDEX idx_ta_teacher_year (teacher_id, academic_year_id)
);
```

---

## 7. Package Organization

```
com.attendai.school.teacherassignment
├── entity
│   ├── TeacherAssignment.java
│   └── AssignmentStatus.java
├── repository
│   └── TeacherAssignmentRepository.java
├── service
│   ├── TeacherAssignmentService.java
│   └── TeacherAssignmentServiceImpl.java
├── controller
│   └── TeacherAssignmentController.java
├── dto
│   ├── CreateTeacherAssignmentRequest.java
│   ├── UpdateTeacherAssignmentRequest.java
│   ├── TeacherAssignmentResponse.java
│   └── TeacherAssignmentSummaryResponse.java
├── mapper
│   └── TeacherAssignmentMapper.java
└── exception
    └── TeacherAssignmentNotFoundException.java
```

---

## 8. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/assignments`

### POST — Create Assignment

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_CREATE`

**Request:**
```json
{
  "sectionId": 10,
  "subjectId": 3,
  "teacherId": 5,
  "isClassTeacher": false
}
```

**Response 201:** `TeacherAssignmentResponse`

---

### GET — List Assignments

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_READ`

**Query params:** `sectionId`, `teacherId`, `subjectId`

**Response 200:** List of `TeacherAssignmentSummaryResponse`

---

### GET /{id}

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_READ`

**Response 200:** `TeacherAssignmentResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_UPDATE`

**Request:** `{ "teacherId": 7, "isClassTeacher": true }`

**Response 200:** `TeacherAssignmentResponse`

---

### PATCH /{id}/status

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_UPDATE`

**Response 200:** `TeacherAssignmentResponse`

---

### DELETE /{id}

**Permission:** `SCHOOL_TEACHER_ASSIGNMENT_DELETE`

**Response 204**

---

## 9. Internal Service API

```
TeacherAssignmentService.getAssignmentsForSection(Long sectionId, Long academicYearId): List<TeacherAssignmentResponse>
TeacherAssignmentService.getClassTeacherForSection(Long sectionId, Long academicYearId): Optional<TeacherAssignmentResponse>
TeacherAssignmentService.existsById(Long id): boolean
```

Used by `school-timetable` and `school-daily-attendance`.

---

## 10. Authorization

| Operation              | Permission                           |
|------------------------|--------------------------------------|
| Create assignment      | `SCHOOL_TEACHER_ASSIGNMENT_CREATE`   |
| Read assignment        | `SCHOOL_TEACHER_ASSIGNMENT_READ`     |
| Update assignment      | `SCHOOL_TEACHER_ASSIGNMENT_UPDATE`   |
| Delete assignment      | `SCHOOL_TEACHER_ASSIGNMENT_DELETE`   |

---

## 11. Flyway Migrations

```
V114__create_school_teacher_assignments_table.sql
```

---

## 12. Acceptance Criteria

- [ ] One subject can have only one active teacher per section per academic year
- [ ] Only one class teacher per section per academic year
- [ ] Teacher must be ACTIVE to be assigned
- [ ] Subject must be associated with the section's class
- [ ] All write operations produce audit log entries

---

## 13. Out of Scope

- Substitute teacher management
- Teacher workload balancing
- Timetable period scheduling (school-timetable)
