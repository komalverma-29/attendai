# Product Overview

**AttendAI** is an AI-powered attendance management platform that automates, standardizes, and intelligently manages attendance across multiple domains — schools, colleges, and enterprises — using a single reusable core engine.

It combines face recognition, rule-based attendance processing, and domain-specific workflows into one cohesive system.

## Architecture Philosophy

- The **Core** is the platform. It has no knowledge of schools, colleges, or enterprises.
- **Business modules** are domain-specific consumers built on top of Core.
- Every new domain must be addable without modifying Core — this is a non-negotiable constraint.

## Version 1 Modules

### attendai-core
The reusable attendance platform engine:
- Authentication and authorization
- User, person, role, and permission management
- Face recognition and face profile management
- Attendance event processing and station management
- Notification engine, file management, audit logging
- System configuration

### attendai-school
School domain module built on top of Core:
- School and administrator management
- Teacher and student management
- Academic year, calendar, class, section, subject, timetable
- Daily attendance with academic calendar awareness
- Attendance rules, corrections, and reports
- Leave management, school dashboard, school settings

## Future Modules

- **attendai-college** — semesters, departments, faculty, courses
- **attendai-enterprise** — employees, departments, shifts, HR workflows

Both will be independent modules on top of Core with zero changes to attendai-core.
