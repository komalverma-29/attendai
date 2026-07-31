# Specification: school-section

## 1. Overview

`school-section` manages the section definitions within a class for a given academic year. A section is a specific division of a class — for example, "Grade 5 – Section A". Students are enrolled into sections, and attendance is recorded at the section level.

Sections are academic-year scoped. Each academic year creates a fresh set of sections, even if the names are identical to the previous year. This preserves historical enrollment and attendance records cleanly.

---

## 2. Scope and Objectives

**In scope:**
- Section creation within a class for a given academic year
- Section status management (ACTIVE, INACTIVE)
- Student enrollment into sections (student-section assignment)
- Roll number assignment within a section
- Listing sections for a class and academic year
- Counting students in a section

**Out of scope:**
- Class definitions (belongs in `school-class`)
- Teacher assignments to sections (belongs in `school-teacher-assignment`)
- Timetable (belongs in `school-timetable`)
- Attendance recording (belongs in `school-daily-attendance`)

---

## 3. Functional Requirements

### FR-SEC-01: Create Section
Create a section for a class within an academic year.

### FR-SEC-02: Get Section by ID
Retrieve a single non-deleted section.

### FR-SEC-03: List Sections for Class and Academic Year
Return all sections for a given class and academic year.

### FR-SEC-04: Update Section
Update section name and description.

### FR-SEC-05: Change Section Status
Activate or deactivate a section.

### FR-SEC-06: Enroll Student into Section
Assign a student to a section for a given academic year with a roll number.

### FR-SEC-07: Remove Student from Section
Remove a student's enrollment from a section. Rejected if the student has attendance records in this section for this year.

### FR-SEC-08: List Students in Section
Return all active students enrolled in a section for an academic year, ordered by roll number.

### FR-SEC-09: Get Student's Section
Return the section a student is enrolled in for a given academic year.

### FR-SEC-10: Delete Section (Soft)
Soft-delete a section. Rejected if students are enrolled or attendance records exist.

---

## 4. Business Rules

- BR-SEC-01: Section name must be unique within a class and academic year (e.g., only one "Section A" per class per year).
- BR-SEC-02: A student can be enrolled in at most one section per academic year.
- BR-SEC-03: Roll number must be unique within a section and academic year.
- BR-SEC-04: A student must be `ACTIVE` to be enrolled in a section.
- BR-SEC-05: An `INACTIVE` section does not accept new student enrollments.
- BR-SEC-06: The academic year must be `ACTIVE` or `UPCOMING` to enroll students (not `COMPLETED`).
- BR-SEC-07: A section with enrolled students or attendance records cannot be soft-deleted.

---

## 5. Domain Model

### SchoolSection Entity

| Field          | Type          | Description                                            |
|----------------|---------------|--------------------------------------------------------|
| id             | Long          | Surrogate PK                                           |
| schoolId       | Long          | FK → school_schools(id), NOT NULL                      |
| classId        | Long          | FK → school_classes(id), NOT NULL                      |
| academicYearId | Long          | FK → school_academic_years(id), NOT NULL               |
| name           | String        | e.g. "A", "B", "Alpha", NOT NULL, max 50               |
| description    | String        | Optional, max 255                                      |
| status         | SectionStatus | Enum: ACTIVE, INACTIVE                                 |
| isDeleted      | boolean       | Soft delete flag                                       |
| deletedAt      | LocalDateTime | Soft delete timestamp                                  |
| createdAt      | LocalDateTime | Audit                                                  |
| updatedAt      | LocalDateTime | Audit                                                  |
| createdBy      | Long          | Audit                                                  |
| updatedBy      | Long          | Audit                                                  |

### SectionEnrollment Entity (student-section mapping)

| Field          | Type          | Description                                            |
|----------------|---------------|--------------------------------------------------------|
| id             | Long          | Surrogate PK                                           |
| sectionId      | Long          | FK → school_sections(id), NOT NULL                     |
| studentId      | Long          | FK → school_students(id), NOT NULL                     |
| academicYearId | Long          | FK → school_academic_years(id), NOT NULL               |
| rollNumber     | String        | Roll number within section, max 20, NOT NULL           |
| enrolledAt     | LocalDate     | Date of enrollment, NOT NULL                           |
| createdAt      | LocalDateTime | Audit                                                  |
| updatedAt      | LocalDateTime | Audit                                                  |
| createdBy      | Long          | Audit                                                  |
| updatedBy      | Long          | Audit                                                  |

---

## 6. Data Model

### Table: `school_sections`

```sql
CREATE TABLE school_sections (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    class_id         BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    name             VARCHAR(50)      NOT NULL,
    description      VARCHAR(255)     NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_sections_school FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_school_sections_class  FOREIGN KEY (class_id)         REFERENCES school_classes(id),
    CONSTRAINT fk_school_sections_year   FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_school_sections_name (class_id, academic_year_id, name),
    INDEX idx_school_sections_class_year (class_id, academic_year_id),
    INDEX idx_school_sections_school (school_id)
);
```

### Table: `school_section_enrollments`

```sql
CREATE TABLE school_section_enrollments (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    section_id       BIGINT UNSIGNED  NOT NULL,
    student_id       BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    roll_number      VARCHAR(20)      NOT NULL,
    enrolled_at      DATE             NOT NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_enrollments_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)       REFERENCES school_students(id),
    CONSTRAINT fk_enrollments_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_enrollments_student_year (student_id, academic_year_id),
    UNIQUE uq_enrollments_roll_number (section_id, academic_year_id, roll_number),
    INDEX idx_enrollments_section (section_id),
    INDEX idx_enrollments_student (student_id)
);
```

---

## 7. Package Organization

```
com.attendai.school.section
├── entity
│   ├── SchoolSection.java
│   ├── SectionEnrollment.java
│   └── SectionStatus.java
├── repository
│   ├── SchoolSectionRepository.java
│   └── SectionEnrollmentRepository.java
├── service
│   ├── SchoolSectionService.java
│   └── SchoolSectionServiceImpl.java
├── controller
│   └── SchoolSectionController.java
├── dto
│   ├── CreateSectionRequest.java
│   ├── UpdateSectionRequest.java
│   ├── EnrollStudentInSectionRequest.java
│   ├── SectionResponse.java
│   ├── SectionSummaryResponse.java
│   └── SectionEnrollmentResponse.java
├── mapper
│   └── SchoolSectionMapper.java
└── exception
    ├── SectionNotFoundException.java
    └── SectionEnrollmentNotFoundException.java
```

---

## 8. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/classes/{classId}/sections`

### POST — Create Section

**Permission:** `SCHOOL_SECTION_CREATE`

**Request:** `{ "name": "A", "description": "Section A" }`

**Response 201:** `SectionResponse`

---

### GET — List Sections

**Permission:** `SCHOOL_SECTION_READ`

**Response 200:** List of `SectionSummaryResponse`

---

### GET /{sectionId}/students — List Students in Section

**Permission:** `SCHOOL_SECTION_READ`

**Response 200:** List of student summaries with roll numbers

---

### POST /{sectionId}/students — Enroll Student

**Permission:** `SCHOOL_SECTION_MANAGE`

**Request:** `{ "studentId": 5, "rollNumber": "01", "enrolledAt": "2025-06-01" }`

**Response 201:** `SectionEnrollmentResponse`

---

### DELETE /{sectionId}/students/{studentId} — Remove Student

**Permission:** `SCHOOL_SECTION_MANAGE`

**Response 204**
**Response 409:** Student has attendance records in this section

---

## 9. Internal Service API

```
SchoolSectionService.findById(Long sectionId): SectionResponse
SchoolSectionService.findStudentEnrollment(Long studentId, Long academicYearId): Optional<SectionEnrollmentResponse>
SchoolSectionService.getStudentsBySection(Long sectionId): List<SectionEnrollmentResponse>
SchoolSectionService.isStudentEnrolledInSection(Long studentId, Long sectionId, Long academicYearId): boolean
```

Used by `school-daily-attendance`, `school-timetable`, and `school-attendance-reports`.

---

## 10. Authorization

| Operation                | Permission                  |
|--------------------------|-----------------------------|
| Create/delete section    | `SCHOOL_SECTION_CREATE/DELETE` |
| Read section             | `SCHOOL_SECTION_READ`       |
| Update section           | `SCHOOL_SECTION_UPDATE`     |
| Enroll/remove student    | `SCHOOL_SECTION_MANAGE`     |

---

## 11. Flyway Migrations

```
V110__create_school_sections_table.sql
V111__create_school_section_enrollments_table.sql
```

---

## 12. Acceptance Criteria

- [ ] Section name is unique within a class and academic year
- [ ] A student can only be enrolled in one section per academic year
- [ ] Roll number is unique within a section and academic year
- [ ] Removing a student with existing attendance records returns 409
- [ ] `INACTIVE` section rejects new student enrollments
- [ ] All enrollment operations produce audit log entries

---

## 13. Out of Scope

- Teacher-section assignments (school-teacher-assignment)
- Subject assignments (school-subject)
- Attendance recording (school-daily-attendance)
