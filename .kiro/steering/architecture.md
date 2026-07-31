# AttendAI — Architecture

## Project Type

Maven Multi-Module Monorepo (Modular Monolith).

All modules live inside one repository and are built together. Each module is a distinct Maven sub-module with its own `pom.xml`, package structure, and bounded context.

---

## Module Structure

```
attend-ai/                          ← Parent POM (aggregator only)
│
├── attendai-core/                  ← Reusable platform engine
│
└── attendai-school/                ← School domain module
```

Future additions:
```
├── attendai-college/               ← Future
└── attendai-enterprise/            ← Future
```

---

## Dependency Rule (Non-Negotiable)

```
attendai-school     →  depends on  →  attendai-core
attendai-college    →  depends on  →  attendai-core    (future)
attendai-enterprise →  depends on  →  attendai-core    (future)

attendai-core       →  depends on  →  NOTHING (no business module)
```

- Core must never import, reference, or depend on any business module.
- Business modules may depend on Core.
- Business modules must never depend on each other.
- Circular dependencies between modules are forbidden.

Violating this rule breaks the extensibility guarantee and must be treated as a critical defect.

---

## attendai-core Responsibilities

Core is the domain-agnostic attendance platform engine. It provides:

| Sub-Module        | Responsibility                                              |
|-------------------|-------------------------------------------------------------|
| `core-auth`       | JWT authentication, token issuance, refresh, revocation     |
| `core-user`       | User accounts, credentials, status management               |
| `core-role`       | Role definitions, role assignment                           |
| `core-permission` | Permission definitions, permission-role mapping             |
| `core-person`     | Generic person entity (name, contact, identity)             |
| `core-face`       | Face profile enrollment, recognition engine integration     |
| `core-attendance` | Attendance event capture, processing, state machine         |
| `core-station`    | Attendance station registration and management              |
| `core-notification` | Notification dispatch (email, push, in-app)               |
| `core-file`       | File upload, storage, retrieval                             |
| `core-audit`      | Audit log creation and querying                             |
| `core-config`     | System configuration key-value store                        |
| `core-common`     | Shared utilities, base classes, exceptions, constants       |

---

## attendai-school Responsibilities

School is the school domain consumer of Core. It adds:

| Sub-Module                      | Responsibility                                         |
|---------------------------------|--------------------------------------------------------|
| `school-school`                 | School entity, school registration, school profile     |
| `school-administrator`          | School administrator management                        |
| `school-teacher`                | Teacher profiles linked to persons and users           |
| `school-student`                | Student enrollment, profiles, roll numbers             |
| `school-academic-year`          | Academic year lifecycle management                     |
| `school-academic-calendar`      | Holidays, working days, calendar rules                 |
| `school-class`                  | Class definitions within a school                      |
| `school-section`                | Sections within a class                                |
| `school-subject`                | Subject definitions                                    |
| `school-timetable`              | Timetable scheduling                                   |
| `school-teacher-assignment`     | Teacher-to-subject-section assignments                 |
| `school-daily-attendance`       | Daily student attendance, calendar-aware               |
| `school-attendance-rules`       | School-specific attendance rules and thresholds        |
| `school-attendance-corrections` | Manual corrections to attendance records               |
| `school-attendance-reports`     | Attendance report generation                           |
| `school-leave`                  | Student and teacher leave management                   |
| `school-dashboard`              | School-level attendance dashboard aggregations         |
| `school-settings`               | School-level configuration overrides                   |
| `school-common`                 | Shared utilities within the school module              |

---

## Layered Architecture (within each module)

Every module follows a strict layered architecture:

```
Controller Layer       ←  HTTP request handling, input validation, response mapping
Service Layer          ←  Business logic, orchestration
Repository Layer       ←  Data access via Spring Data JPA
Domain / Entity Layer  ←  JPA entities, domain objects
DTO Layer              ←  Request/Response transfer objects
Mapper Layer           ←  MapStruct mappings between entities and DTOs
Exception Layer        ←  Module-specific exceptions
```

Rules:
- Controllers must not contain business logic.
- Services must not build HTTP responses.
- Repositories must not contain business logic.
- Entities must not be exposed directly in API responses.
- DTOs must not contain JPA annotations.

---

## Package Naming Convention

### Core
```
com.attendai.core.<module>.<layer>
```
Examples:
```
com.attendai.core.auth.controller
com.attendai.core.auth.service
com.attendai.core.auth.repository
com.attendai.core.auth.entity
com.attendai.core.auth.dto
com.attendai.core.auth.mapper
com.attendai.core.auth.exception
```

### School
```
com.attendai.school.<module>.<layer>
```
Examples:
```
com.attendai.school.student.controller
com.attendai.school.student.service
com.attendai.school.student.repository
com.attendai.school.student.entity
com.attendai.school.student.dto
com.attendai.school.student.mapper
com.attendai.school.student.exception
```

---

## Maven Module Naming Convention

| Module                 | Maven artifactId         |
|------------------------|--------------------------|
| Parent aggregator      | `attendai-parent`        |
| Core module            | `attendai-core`          |
| School module          | `attendai-school`        |
| College module (future)| `attendai-college`       |
| Enterprise (future)    | `attendai-enterprise`    |

---

## Prohibited Concepts in attendai-core

Core must never contain any of the following concepts, entities, or references:

- Student, Teacher, Administrator, Principal
- School, College, University, Campus
- Parent, Guardian
- Class, Section, Grade, Room
- Subject, Course, Module (academic)
- Academic Year, Semester, Term
- Department, Faculty, Division
- Employee, Staff, Manager, Executive
- Admission Number, Roll Number, Employee ID
- Timetable, Schedule (domain-specific)
- Leave (domain-specific policy)

If a concept is domain-specific, it belongs in the domain module — not in Core.

---

## Cross-Cutting Concerns

These are handled centrally and consistently across all modules:

| Concern            | Mechanism                                      |
|--------------------|------------------------------------------------|
| Authentication     | Spring Security + JWT (Core)                   |
| Authorization      | Method-level `@PreAuthorize` with permissions  |
| Exception handling | `@RestControllerAdvice` per module             |
| Validation         | Jakarta Bean Validation on DTOs                |
| Audit logging      | Core audit module (AOP or service call)        |
| Transaction mgmt   | `@Transactional` on service methods            |
| Logging            | SLF4J + Logback                                |

---

## REST API Design

- All APIs follow REST conventions.
- Base path pattern: `/api/v1/<module>/<resource>`
- HTTP methods used semantically: GET, POST, PUT, PATCH, DELETE.
- Responses use consistent envelope structure from `core-common`.
- Pagination uses `page` and `size` query parameters.
- Error responses follow a standard error body structure.

---

## Configuration

- All environment-specific configuration lives in `application.yml` (not `application.properties`).
- Secrets must never be hardcoded. Use environment variables or a secrets manager.
- Module-specific configuration uses `@ConfigurationProperties` with typed classes.
- Profiles: `dev`, `test`, `prod`.
