# AttendAI — Product Vision

## What AttendAI Is

AttendAI is an AI-powered attendance management platform designed to automate, standardize, and intelligently manage attendance across multiple domains — schools, colleges, and enterprises — using a single reusable core engine.

The platform combines face recognition, rule-based attendance processing, and domain-specific workflows into one cohesive system.

---

## Mission

Build a reliable, extensible, and domain-agnostic attendance platform that any organization can adopt without requiring changes to the core engine.

---

## Design Philosophy

- The **Core** is the platform. It knows nothing about schools, colleges, or enterprises.
- **Business modules** (School, College, Enterprise) are consumers of the Core. They bring domain-specific knowledge on top of a stable foundation.
- Every new domain should be addable without touching Core.

---

## Version 1 Scope

Version 1 delivers two modules:

### attendai-core
The reusable attendance platform engine. Includes:
- Authentication and authorization
- User and person management
- Role and permission management
- Face recognition and face profile management
- Attendance event processing
- Attendance station management
- Notification engine
- File management
- Audit logging
- System configuration

### attendai-school
The school domain module built on top of Core. Includes:
- School and administrator management
- Teacher and student management
- Academic year, academic calendar, class, section, subject, timetable
- Teacher assignments
- Daily attendance with academic calendar awareness
- Attendance rules and corrections
- Attendance reports
- Leave management
- School dashboard
- School settings

---

## Future Scope

- **attendai-college** — College domain (semesters, departments, faculty, courses)
- **attendai-enterprise** — Enterprise domain (employees, departments, shifts, HR workflows)

Both will be built as independent modules on top of the same Core, without modifying it.

---

## Core Stability Guarantee

The Core must remain stable across all domain expansions. Adding College or Enterprise must never require a change to attendai-core. This is a non-negotiable architectural constraint.
