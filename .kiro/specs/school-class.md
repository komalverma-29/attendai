# Specification: school-class

## 1. Overview

`school-class` manages the class (grade) definitions within a school. A class represents a grade level — such as "Grade 1", "Class 5", or "Standard 10". Classes are the organizational backbone from which sections are created.

Classes are school-scoped and persist across academic years. They define the academic grade structure of the school and are referenced by sections, timetables, and attendance rules.

---

## 2. Scope and Objectives

**In scope:**
- Class definition CRUD within a school
- Class ordering (grade order / sequence)
- Class status management (ACTIVE, INACTIVE)
- Soft delete with dependency guard

**Out of scope:**
- Section management (belongs in `school-section`)
- Subject assignment to classes (belongs in `school-subject`)
- Timetable (belongs in `school-timetable`)

---

## 3. Functional Requirements

### FR-CLASS-01: Create Class
Create a class for a school with a name, display name, and grade order.

### FR-CLASS-02: Get Class by ID
Retrieve a single non-deleted class.

### FR-CLASS-03: List Classes for School
Return all classes for a school ordered by `gradeOrder`. Filterable by status.

### FR-CLASS-04: Update Class
Update class name, display name, and grade order.

### FR-CLASS-05: Change Class Status
Activate or deactivate a class.

### FR-CLASS-06: Delete Class (Soft)
Soft-delete a class. Rejected if the class has any sections.

---

## 4. Business Rules

- BR-CLASS-01: Class name must be unique within a school.
- BR-CLASS-02: `gradeOrder` defines the sequence when listing classes (Grade 1 = 1, Grade 2 = 2, etc.).
- BR-CLASS-03: An `INACTIVE` class cannot have new sections created under it.
- BR-CLASS-04: A class with sections cannot be soft-deleted.

---

## 5. Domain Model

### SchoolClass Entity

| Field       | Type          | Description                                          |
|-------------|---------------|------------------------------------------------------|
| id          | Long          | Surrogate PK                                         |
| schoolId    | Long          | FK → school_schools(id), NOT NULL                    |
| name        | String        | e.g. "Grade 5", NOT NULL, unique within school, max 100 |
| displayName | String        | Optional alternate display name, max 100             |
| gradeOrder  | int           | Numeric ordering, NOT NULL                           |
| status      | ClassStatus   | Enum: ACTIVE, INACTIVE                               |
| isDeleted   | boolean       | Soft delete flag                                     |
| deletedAt   | LocalDateTime | Soft delete timestamp                                |
| createdAt   | LocalDateTime | Audit                                                |
| updatedAt   | LocalDateTime | Audit                                                |
| createdBy   | Long          | Audit                                                |
| updatedBy   | Long          | Audit                                                |

---

## 6. Data Model

### Table: `school_classes`

```sql
CREATE TABLE school_classes (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id    BIGINT UNSIGNED  NOT NULL,
    name         VARCHAR(100)     NOT NULL,
    display_name VARCHAR(100)     NULL,
    grade_order  INT              NOT NULL,
    status       VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted   BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME         NULL,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_classes_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_classes_name (school_id, name),
    INDEX idx_school_classes_school (school_id),
    INDEX idx_school_classes_order (school_id, grade_order)
);
```

---

## 7. Package Organization

```
com.attendai.school.schoolclass
├── entity
│   ├── SchoolClass.java
│   └── ClassStatus.java
├── repository
│   └── SchoolClassRepository.java
├── service
│   ├── SchoolClassService.java
│   └── SchoolClassServiceImpl.java
├── controller
│   └── SchoolClassController.java
├── dto
│   ├── CreateClassRequest.java
│   ├── UpdateClassRequest.java
│   ├── ClassResponse.java
│   └── ClassSummaryResponse.java
├── mapper
│   └── SchoolClassMapper.java
└── exception
    └── ClassNotFoundException.java
```

---

## 8. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/classes`

### POST — Create Class

**Permission:** `SCHOOL_CLASS_CREATE`

**Request:**
```json
{ "name": "Grade 5", "displayName": "5th Standard", "gradeOrder": 5 }
```

**Response 201:** `ClassResponse`

---

### GET — List Classes

**Permission:** `SCHOOL_CLASS_READ`

**Query params:** `status`

**Response 200:** List of `ClassSummaryResponse` ordered by `gradeOrder`

---

### GET /{id}

**Permission:** `SCHOOL_CLASS_READ`

**Response 200:** `ClassResponse`

---

### PUT /{id}

**Permission:** `SCHOOL_CLASS_UPDATE`

**Response 200:** `ClassResponse`

---

### PATCH /{id}/status

**Permission:** `SCHOOL_CLASS_UPDATE`

**Response 200:** `ClassResponse`

---

### DELETE /{id}

**Permission:** `SCHOOL_CLASS_DELETE`

**Response 204**
**Response 409:** Class has sections

---

## 9. Internal Service API

```
SchoolClassService.existsById(Long classId): boolean
SchoolClassService.findByIdOrThrow(Long classId): ClassResponse
SchoolClassService.isActive(Long classId): boolean
```

Used by `school-section` and `school-subject` for validation.

---

## 10. Authorization

| Operation     | Permission              |
|---------------|-------------------------|
| Create class  | `SCHOOL_CLASS_CREATE`   |
| Read class    | `SCHOOL_CLASS_READ`     |
| Update class  | `SCHOOL_CLASS_UPDATE`   |
| Delete class  | `SCHOOL_CLASS_DELETE`   |

---

## 11. Flyway Migrations

```
V109__create_school_classes_table.sql
```

---

## 12. Acceptance Criteria

- [ ] Class name is unique within a school
- [ ] Classes are listed in `gradeOrder` ascending sequence
- [ ] A class with sections cannot be deleted
- [ ] `INACTIVE` class returns `isActive() = false`
- [ ] All CRUD operations produce audit log entries

---

## 13. Out of Scope

- Section management (school-section)
- Subject definitions (school-subject)
- Class-level timetabling
