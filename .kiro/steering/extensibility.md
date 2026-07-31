# AttendAI — Extensibility and Future Modules

## Design Goal

The AttendAI platform is designed to support multiple business domains on top of a single reusable core engine. The architecture must allow new domains to be added as independent modules without modifying `attendai-core` or any existing business module.

---

## Planned Future Modules

### attendai-college

A college domain module providing:
- Faculty and department management
- Semester and academic term management
- Course and curriculum management
- Student registration per semester
- College-specific attendance rules (lecture attendance percentage)
- Timetable and academic calendar for colleges
- Leave management for faculty and students
- College dashboard and reports

### attendai-enterprise

An enterprise/workforce domain module providing:
- Employee and department management
- Shift and work schedule management
- HR-integrated attendance workflows
- Leave and time-off management
- Payroll attendance data export
- Location-based or device-based check-in
- Enterprise dashboard and compliance reports

---

## How New Modules Are Added

Adding a new domain requires only:

1. Create a new Maven sub-module (e.g., `attendai-college`).
2. Add `attendai-core` as a dependency in the new module's `pom.xml`.
3. Add the new module to the parent `pom.xml` `<modules>` section.
4. Implement domain-specific functionality within the new module's package namespace (`com.attendai.college.<module>.<layer>`).
5. Write Flyway migrations for the new module's tables (prefixed with the domain, e.g., `college_` tables).
6. Register the new module's Spring component scan if running as a combined application.

No changes to `attendai-core` are permitted.
No changes to `attendai-school` are permitted.

---

## Core Contracts That Enable Extensibility

These Core abstractions are the extension points that new modules consume:

| Core Contract           | What It Provides                                         |
|-------------------------|----------------------------------------------------------|
| `core-auth`             | JWT-based authentication usable by any module            |
| `core-user`             | User accounts that any domain can link to a person       |
| `core-person`           | Generic person record any domain maps to (student, employee, faculty) |
| `core-role`             | Roles that can be scoped per domain module               |
| `core-permission`       | Permission codes namespaced by module                    |
| `core-attendance`       | Attendance event model any domain feeds into             |
| `core-station`          | Station hardware abstracted from domain logic            |
| `core-face`             | Face recognition behind an interface, domain-agnostic    |
| `core-notification`     | Notification dispatch any module can trigger             |
| `core-audit`            | Audit logging any module can write to                    |
| `core-file`             | File storage any module can use                          |
| `core-config`           | Configuration keys any module can define and read        |

---

## Rules for Core Stability

These rules ensure Core remains stable as new modules are added:

1. **Core must not know about any domain module.** No imports, no compile-time references, no Spring bean wiring from Core to a business module.

2. **Core domain model must remain generic.** If a concept only makes sense in one domain, it must not be added to Core. It belongs in that domain's module.

3. **Core APIs must be stable.** Once a Core service interface is published, breaking changes require a versioning strategy (e.g., new method on the interface with a default implementation, or a new interface version).

4. **Core database tables must remain unprefixed and generic.** Tables like `users`, `persons`, `roles` are Core-owned. Domain modules add their own prefixed tables and reference Core tables via foreign keys.

5. **Core Spring beans must not be scoped to a domain.** Core configuration, filters, and services must work correctly regardless of which domain modules are present.

---

## Extension Pattern: Domain Person Linking

Each domain module creates its own domain entity and links it to the Core `Person` entity via a foreign key. Core never knows about this link.

```
Core:    persons (id, first_name, last_name, ...)
              ↑ FK
School:  school_students (id, person_id, roll_number, ...)
              ↑ FK
College: college_students (id, person_id, student_id, ...)
              ↑ FK
Enterprise: enterprise_employees (id, person_id, employee_code, ...)
```

This pattern keeps Core generic while allowing each domain to extend person data with domain-specific fields.

---

## Extension Pattern: Domain Attendance Rules

Core records attendance events. Each domain module implements its own attendance rule engine on top of Core events.

```
Core:    attendance_events (id, person_id, station_id, event_time, ...)
              ↑ consumed by
School:  school_attendance_rules (applies school-specific logic)
College: college_attendance_rules (applies lecture percentage logic)
Enterprise: enterprise_attendance_rules (applies shift logic)
```

Core does not know about any of these rule implementations.

---

## Extension Pattern: Permissions

Permission codes are namespaced by module. Each domain module defines and seeds its own permission codes in the `permissions` table (Core-owned table), using its own namespace prefix.

```
Core permissions:       CORE_USER_MANAGE, CORE_ROLE_MANAGE
School permissions:     SCHOOL_STUDENT_CREATE, SCHOOL_ATTENDANCE_MARK
College permissions:    COLLEGE_FACULTY_MANAGE, COLLEGE_ATTENDANCE_VIEW
Enterprise permissions: ENTERPRISE_EMPLOYEE_MANAGE, ENTERPRISE_SHIFT_ASSIGN
```

This allows Core's RBAC system to enforce permissions across all domains without Core knowing the domain-specific permission codes.

---

## Checklist for Adding a New Module

When adding `attendai-college` or `attendai-enterprise`, verify:

- [ ] New Maven sub-module created with its own `pom.xml`
- [ ] Parent `pom.xml` updated to include new module
- [ ] `attendai-core` declared as dependency in new module
- [ ] Package namespace follows `com.attendai.<domain>.<module>.<layer>`
- [ ] No dependency on `attendai-school` or other business modules
- [ ] Flyway migrations use domain-specific table prefix
- [ ] Domain entities link to Core `person` via FK, not via Java class reference
- [ ] Permission codes use domain namespace prefix
- [ ] No Core classes modified
- [ ] No School classes modified
- [ ] Security configuration extended (not replaced) to include new public endpoints if any
