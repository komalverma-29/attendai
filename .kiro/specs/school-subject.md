# Specification: school-subject

## 1. Overview

`school-subject` manages subject definitions within a school. A subject represents a curriculum area taught to students — such as Mathematics, English, Physics, or Physical Education.

Subjects are school-scoped and persist across academic years. They are referenced by teacher assignments (which teacher teaches which subject in which section) and timetables (which subject is scheduled in which period).

---

## 2. Scope and Objectives

**In scope:**
- Subject CRUD within a school
- Subject type classification (ACADEMIC, LANGUAGE, PRACTICAL, CO_CURRICULAR)
- Associating subjects with classes (which subjects are taught in which grade)
- Subject status management
- Soft delete with dependency guard

**Out of scope:**
- Syllabus or curriculum content
- Exam marks or grades
- Teacher-subject-section assignment (belongs in `school-teacher-assignment`)
- Timetable period scheduling (belongs in `school-timetable`)

---

## 3. Functional Requirements

### FR-SUB-01: Create Subject
Create a subject for a school with a name, code, type, and optional description.

### FR-SUB-02: Get Subject by ID
Retrieve a single non-deleted subject.

### FR-SUB-03: List Subjects for School
Return all subjects for a school. Filterable by type and status.

### FR-SUB-04: Update Subject
Update name, description, and type.

### FR-SUB-05: Change Subject Status
Activate or deactivate a subject.

### FR-SUB-06: Assign Subject to Class
Associate a subject with a class, meaning that subject is offered for that grade level.

### FR-SUB-07: Remove Subject from Class
Remove a class-subject association. Rejected if active teacher assignments exist for this subject-class combination.

### FR-SUB-08: Get Subjects for Class
List all subjects associated with a given class.

### FR-SUB-09: Delete Subject (Soft)
Soft-delete a subject. Rejected if active teacher assignments or timetable entries reference it.

---

## 4. Business Rules

- BR-SUB-01: Subject code must be unique within a school (e.g. "MATH", "ENG", "PHY").
- BR-SUB-02: Subject name must be unique within a school.
- BR-SUB-03: A subject can be associated with multiple classes.
- BR-SUB-04: An `INACTIVE` subject cannot be assigned to new teacher assignments or timetable slots.
- BR-SUB-05: A subject with active teacher assignments or timetable entries cannot be soft-deleted.

---

## 5. Domain Model

### SchoolSubject Entity

| Field       | Type          | Description                                            |
|-------------|---------------|--------------------------------------------------------|
| id          | Long          | Surrogate PK                                           |
| schoolId    | Long          | FK → school_schools(id), NOT NULL                      |
| name        | String        | NOT NULL, unique within school, max 200                |
| code        | String        | NOT NULL, unique within school, uppercase, max 20      |
| type        | SubjectType   | Enum: ACADEMIC, LANGUAGE, PRACTICAL, CO_CURRICULAR     |
| description | String        | Optional, max 500                                      |
| status      | SubjectStatus | Enum: ACTIVE, INACTIVE                                 |
| isDeleted   | boolean       | Soft delete flag                                       |
| deletedAt   | LocalDateTime | Soft delete timestamp                                  |
| createdAt   | LocalDateTime | Audit                                                  |
| updatedAt   | LocalDateTime | Audit                                                  |
| createdBy   | Long          | Audit                                                  |
| updatedBy   | Long          | Audit                                                  |

### ClassSubject Entity (class-subject mapping)

| Field     | Type          | Description                            |
|-----------|---------------|----------------------------------------|
| id        | Long          | Surrogate PK                           |
| classId   | Long          | FK → school_classes(id), NOT NULL      |
| subjectId | Long          | FK → school_subjects(id), NOT NULL     |
| createdAt | LocalDateTime | Audit                                  |
| createdBy | Long          | Audit                                  |

---

## 6. Data Model

### Table: `school_subjects`

```sql
CREATE TABLE school_subjects (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(200)     NOT NULL,
    code        VARCHAR(20)      NOT NULL,
    type        VARCHAR(30)      NOT NULL,
    description VARCHAR(500)     NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_subjects_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_subjects_name (school_id, name),
    UNIQUE uq_school_subjects_code (school_id, code),
    INDEX idx_school_subjects_school (school_id)
);
```

### Table: `school_class_subjects`

```sql
CREATE TABLE school_class_subjects (
    id         BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    class_id   BIGINT UNSIGNED  NOT NULL,
    subject_id BIGINT UNSIGNED  NOT NULL,
    created_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_class_subjects_class   FOREIGN KEY (class_id)   REFERENCES school_classes(id),
    CONSTRAINT fk_class_subjects_subject FOREIGN KEY (subject_id) REFERENCES school_subjects(id),
    UNIQUE uq_class_subjects (class_id, subject_id),
    INDEX idx_class_subjects_class (class_id),
    INDEX idx_class_subjects_subject (subject_id)
);
```

---

## 7. Package Organization

```
com.attendai.school.subject
├── entity
│   ├── SchoolSubject.java
│   ├── ClassSubject.java
│   ├── SubjectType.java
│   └── SubjectStatus.java
├── repository
│   ├── SchoolSubjectRepository.java
│   └── ClassSubjectRepository.java
├── service
│   ├── SchoolSubjectService.java
│   └── SchoolSubjectServiceImpl.java
├── controller
│   └── SchoolSubjectController.java
├── dto
│   ├── CreateSubjectRequest.java
│   ├── UpdateSubjectRequest.java
│   ├── SubjectResponse.java
│   └── SubjectSummaryResponse.java
├── mapper
│   └── SchoolSubjectMapper.java
└── exception
    └── SubjectNotFoundException.java
```

---

## 8. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/subjects`

### POST — Create Subject

**Permission:** `SCHOOL_SUBJECT_CREATE`

**Request:**
```json
{
  "name": "Mathematics",
  "code": "MATH",
  "type": "ACADEMIC",
  "description": "Core mathematics curriculum"
}
```

**Response 201:** `SubjectResponse`

---

### GET — List Subjects

**Permission:** `SCHOOL_SUBJECT_READ`

**Query params:** `type`, `status`, `classId`

**Response 200:** List of `SubjectSummaryResponse`

---

### GET /{id}

**Permission:** `SCHOOL_SUBJECT_READ`

**Response 200:** `SubjectResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_SUBJECT_UPDATE`

**Response 200:** `SubjectResponse`

---

### PATCH /{id}/status

**Permission:** `SCHOOL_SUBJECT_UPDATE`

**Response 200:** `SubjectResponse`

---

### POST /{id}/classes — Assign to Class

**Permission:** `SCHOOL_SUBJECT_UPDATE`

**Request:** `{ "classId": 3 }`

**Response 200**

---

### DELETE /{id}/classes/{classId}

**Permission:** `SCHOOL_SUBJECT_UPDATE`

**Response 204**
**Response 409:** Active teacher assignments exist

---

### DELETE /{id}

**Permission:** `SCHOOL_SUBJECT_DELETE`

**Response 204**

---

## 9. Internal Service API

```
SchoolSubjectService.findByIdOrThrow(Long subjectId): SubjectResponse
SchoolSubjectService.existsById(Long subjectId): boolean
SchoolSubjectService.getSubjectsByClassId(Long classId): List<SubjectSummaryResponse>
```

Used by `school-teacher-assignment` and `school-timetable`.

---

## 10. Authorization

| Operation       | Permission               |
|-----------------|--------------------------|
| Create subject  | `SCHOOL_SUBJECT_CREATE`  |
| Read subject    | `SCHOOL_SUBJECT_READ`    |
| Update subject  | `SCHOOL_SUBJECT_UPDATE`  |
| Delete subject  | `SCHOOL_SUBJECT_DELETE`  |

---

## 11. Flyway Migrations

```
V112__create_school_subjects_table.sql
V113__create_school_class_subjects_table.sql
```

---

## 12. Acceptance Criteria

- [ ] Subject code is unique within a school
- [ ] Subject name is unique within a school
- [ ] Subject with active teacher assignments cannot be deleted
- [ ] `getSubjectsByClassId` returns only subjects linked to that class
- [ ] All CRUD operations produce audit log entries

---

## 13. Out of Scope

- Exam marks or curriculum content
- Teacher-subject-section assignments (school-teacher-assignment)
- Timetable period scheduling (school-timetable)
