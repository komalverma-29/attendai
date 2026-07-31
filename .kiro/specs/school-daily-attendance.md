# Specification: school-daily-attendance

## 1. Overview

`school-daily-attendance` is the core business logic layer of the School attendance module. It consumes raw attendance events from `core-attendance`, applies school-specific rules (via `school-attendance-rules`), cross-references the academic calendar, and produces a definitive daily attendance record for each student.

The result of processing is a `DailyAttendanceRecord` — a single authoritative record per student per day stating whether the student was PRESENT, ABSENT, LATE, or ON_LEAVE. This record is what drives attendance reports, parent notifications, and shortage warnings.

---

## 2. Scope and Objectives

**In scope:**
- Polling `core-attendance` for PENDING events belonging to school students
- Validating each event against the academic calendar (must be a working day)
- Applying arrival-time rules to classify students as PRESENT or LATE
- Producing `DailyAttendanceRecord` rows per student per date
- Marking ABSENT students who have no event by end of school day
- Notifying parents/guardians of absent students (via `core-notification`)
- Querying attendance records by section, student, and date range
- Providing the data foundation for attendance reports and corrections

**Out of scope:**
- Attendance rule definitions (belongs in `school-attendance-rules`)
- Manual corrections to finalized records (belongs in `school-attendance-corrections`)
- Report generation (belongs in `school-attendance-reports`)
- Leave management (belongs in `school-leave`)

---

## 3. Functional Requirements

### FR-DA-01: Process Pending Attendance Events
A scheduled job polls `core-attendance` for PENDING events where the person is a school student. For each event, it:
1. Confirms the student is enrolled in an active section.
2. Confirms the event date is a working day (via `school-academic-calendar`).
3. Applies arrival-time rules to determine PRESENT or LATE status.
4. Creates or updates the student's `DailyAttendanceRecord` for that date.
5. Marks the Core event as PROCESSED via `AttendanceService.markAsProcessed()`.

### FR-DA-02: Mark Absent Students
A scheduled job runs after the school's configured "mark-absent time" (e.g., 11:00 AM). For every active student enrolled in a section on a working day who has no attendance record for that date, it creates an ABSENT record.

### FR-DA-03: Get Daily Attendance for Section
Return the attendance record for all students in a section for a specific date.

### FR-DA-04: Get Daily Attendance for Student
Return all attendance records for a student within a date range.

### FR-DA-05: Get Attendance Summary for Section
Return a summary of PRESENT, ABSENT, LATE, ON_LEAVE counts for a section on a given date.

### FR-DA-06: Notify Absent Students
When a student is marked ABSENT, trigger a notification to the guardian (if guardian contact is available) via `core-notification` using the `SCHOOL_STUDENT_ABSENT` notification type.

### FR-DA-07: Override Record Status
Allow an authorized user to directly override a `DailyAttendanceRecord` status (e.g., change ABSENT to PRESENT after manual verification). This creates an audit trail but is simpler than a full correction — corrections with reasons are handled by `school-attendance-corrections`.

---

## 4. Non-Functional Requirements

- The event-processing job runs every 5 minutes during school hours (configurable via `school-settings`).
- The mark-absent job runs once per day at the configured mark-absent time.
- Processing a single event must complete within 50ms.
- `DailyAttendanceRecord` queries must return within 100ms for a section of up to 60 students.
- The system must be idempotent: re-processing an already-processed event must not create duplicate records.

---

## 5. Business Rules

- BR-DA-01: Attendance can only be recorded for dates that are working days per the academic calendar.
- BR-DA-02: Only `ACTIVE` students enrolled in a section can have attendance records.
- BR-DA-03: Only one `DailyAttendanceRecord` exists per student per date. Duplicate processing is silently ignored (idempotent).
- BR-DA-04: A student on approved leave for a date receives an `ON_LEAVE` status, not `ABSENT`. `school-leave` sets this status when leave is approved.
- BR-DA-05: The arrival-time rule is loaded from `school-attendance-rules` for the student's section/academic-year. If no rule is configured, all arrivals are marked PRESENT.
- BR-DA-06: A student who arrives after the LATE threshold time is marked LATE, not ABSENT.
- BR-DA-07: A student with a LATE record still counts as attending for minimum attendance calculations.
- BR-DA-08: Attendance records for COMPLETED academic years are read-only.

---

## 6. Attendance Status Values

| Status    | Meaning                                                            |
|-----------|--------------------------------------------------------------------|
| `PRESENT` | Student arrived on time                                            |
| `LATE`    | Student arrived after the late threshold                           |
| `ABSENT`  | Student did not arrive; no event recorded by mark-absent time      |
| `ON_LEAVE`| Student was on approved leave (set by school-leave)               |

---

## 7. Processing Flow (Event-Driven)

```
[Scheduled Job — every 5 minutes]
    │
    ▼
AttendanceService.findPendingEventsForPerson(personId, date)
  [for each student in active sections of this school]
    │
    ▼
For each PENDING event:
  1. Resolve student from personId
     └── SchoolStudentService.findByPersonId(personId)
  2. Confirm student is ACTIVE and enrolled in a section
  3. Confirm academicCalendar.isWorkingDay(schoolId, academicYearId, eventDate)
     └── If NOT working day → skip (mark event PROCESSED, no record created)
  4. Load AttendanceRule for section/academicYear
  5. Determine status:
     └── eventTime.toLocalTime() ≤ lateThresholdTime → PRESENT
     └── eventTime.toLocalTime() > lateThresholdTime → LATE
  6. Create DailyAttendanceRecord (or update if already exists as ABSENT)
  7. AttendanceService.markAsProcessed(eventId, "school")
  8. Write audit log
```

---

## 8. Mark-Absent Flow

```
[Scheduled Job — runs once at school's mark-absent time]
    │
    ▼
For each ACTIVE school:
  Get active academic year
  Confirm today is a working day
  Get all active section enrollments for this academic year
  For each student with no DailyAttendanceRecord for today:
    If student has approved leave today → status = ON_LEAVE (already set by school-leave)
    Else → create DailyAttendanceRecord(status = ABSENT)
    Trigger absence notification via NotificationService
```

---

## 9. Domain Model

### DailyAttendanceRecord Entity

| Field           | Type                  | Description                                                    |
|-----------------|-----------------------|----------------------------------------------------------------|
| id              | Long                  | Surrogate PK                                                   |
| schoolId        | Long                  | FK → school_schools(id), NOT NULL                              |
| academicYearId  | Long                  | FK → school_academic_years(id), NOT NULL                       |
| sectionId       | Long                  | FK → school_sections(id), NOT NULL                             |
| studentId       | Long                  | FK → school_students(id), NOT NULL                             |
| attendanceDate  | LocalDate             | The date, NOT NULL                                             |
| status          | DailyAttendanceStatus | Enum: PRESENT, LATE, ABSENT, ON_LEAVE                          |
| arrivalTime     | LocalTime             | When the student arrived, nullable                             |
| coreEventId     | Long                  | FK → attendance_events(id) [Core], nullable                    |
| remarks         | String                | Optional remarks, max 500                                      |
| markedById      | Long                  | userId who set/overrode this record, nullable (null = system)  |
| createdAt       | LocalDateTime         | Audit                                                          |
| updatedAt       | LocalDateTime         | Audit                                                          |
| createdBy       | Long                  | Audit                                                          |
| updatedBy       | Long                  | Audit                                                          |

---

## 10. Data Model

### Table: `school_daily_attendance`

```sql
CREATE TABLE school_daily_attendance (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    student_id       BIGINT UNSIGNED  NOT NULL,
    attendance_date  DATE             NOT NULL,
    status           VARCHAR(20)      NOT NULL,
    arrival_time     TIME             NULL,
    core_event_id    BIGINT UNSIGNED  NULL,
    remarks          VARCHAR(500)     NULL,
    marked_by_id     BIGINT UNSIGNED  NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_daily_att_school  FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_daily_att_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_daily_att_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_daily_att_student FOREIGN KEY (student_id)       REFERENCES school_students(id),
    UNIQUE uq_daily_att_student_date (student_id, attendance_date),
    INDEX idx_daily_att_section_date (section_id, attendance_date),
    INDEX idx_daily_att_student_date (student_id, attendance_date),
    INDEX idx_daily_att_school_date (school_id, attendance_date),
    INDEX idx_daily_att_status (status)
);
```

---

## 11. Package Organization

```
com.attendai.school.dailyattendance
├── entity
│   ├── DailyAttendanceRecord.java
│   └── DailyAttendanceStatus.java
├── repository
│   └── DailyAttendanceRepository.java
├── service
│   ├── DailyAttendanceService.java
│   └── DailyAttendanceServiceImpl.java
├── scheduler
│   ├── AttendanceProcessingJob.java
│   └── MarkAbsentJob.java
├── controller
│   └── DailyAttendanceController.java
├── dto
│   ├── DailyAttendanceRecordResponse.java
│   ├── SectionAttendanceSummaryResponse.java
│   ├── OverrideAttendanceRequest.java
│   └── StudentAttendanceDayResponse.java
├── mapper
│   └── DailyAttendanceMapper.java
└── exception
    └── AttendanceRecordNotFoundException.java
```

---

## 12. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/attendance`

### GET /sections/{sectionId}/daily — Get Section Attendance for Date

**Permission:** `SCHOOL_ATTENDANCE_READ`

**Query params:** `date` (ISO date, required)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "sectionId": 10,
    "date": "2025-09-01",
    "isWorkingDay": true,
    "records": [
      { "studentId": 5, "rollNumber": "01", "fullName": "Arjun Patel", "status": "PRESENT", "arrivalTime": "08:45" },
      { "studentId": 6, "rollNumber": "02", "fullName": "Priya Sharma", "status": "ABSENT", "arrivalTime": null }
    ],
    "summary": { "present": 25, "absent": 3, "late": 2, "onLeave": 1 }
  }
}
```

---

### GET /students/{studentId} — Get Student Attendance

**Permission:** `SCHOOL_ATTENDANCE_READ`

**Query params:** `fromDate`, `toDate`, `academicYearId`

**Response 200:** List of `DailyAttendanceRecordResponse`

---

### PATCH /records/{id}/override — Override Record Status

**Permission:** `SCHOOL_ATTENDANCE_OVERRIDE`

**Request:**
```json
{
  "status": "PRESENT",
  "remarks": "Student was present; station malfunction on entry"
}
```

**Response 200:** `DailyAttendanceRecordResponse`

---

### GET /sections/{sectionId}/summary — Section Attendance Summary

**Permission:** `SCHOOL_ATTENDANCE_READ`

**Query params:** `fromDate`, `toDate`

**Response 200:** Aggregated summary per date

---

## 13. Validation Rules

| Field          | Rule                                                               |
|----------------|--------------------------------------------------------------------|
| date (query)   | Valid date, within active academic year range                      |
| override status| Valid `DailyAttendanceStatus` value                                |
| remarks        | Optional, max 500                                                  |

---

## 14. Authorization

| Operation                  | Permission                      |
|----------------------------|---------------------------------|
| Read attendance             | `SCHOOL_ATTENDANCE_READ`        |
| Override record             | `SCHOOL_ATTENDANCE_OVERRIDE`    |
| Process events (scheduled)  | System job — no HTTP auth       |

---

## 15. Configuration

| Setting Key                                | Default | Description                                 |
|--------------------------------------------|---------|---------------------------------------------|
| `school.attendance.processing-interval-minutes` | `5` | Event polling frequency during school hours |
| `school.attendance.mark-absent-time`       | `11:00` | Time of day to mark absent students         |
| `school.attendance.notify-absent`          | `true`  | Send notification on absent mark            |

These are stored in `school-settings` (school-scoped) and override `core-config` defaults.

---

## 16. Integration Points

| Module                     | Integration                                                        |
|----------------------------|--------------------------------------------------------------------|
| `core-attendance`          | `findPendingEventsForPerson`, `markAsProcessed` — primary input    |
| `school-academic-calendar` | `isWorkingDay()` — mandatory gate before processing                |
| `school-attendance-rules`  | Load late threshold time for section/academic year                 |
| `school-section`           | `getStudentsBySection()` for mark-absent job                       |
| `school-student`           | `findByPersonId()` to resolve student from Core person             |
| `school-academic-year`     | `getActiveAcademicYearOrThrow()` for scheduling context            |
| `school-leave`             | Checks approved leave before marking ABSENT                        |
| `core-notification`        | Sends `SCHOOL_STUDENT_ABSENT` notification to guardian             |
| `core-audit`               | Audit log for all record creates and overrides                     |

---

## 17. Error Handling

| Scenario                             | Handling                                         |
|--------------------------------------|--------------------------------------------------|
| Event for non-school person          | Skip; mark Core event PROCESSED                  |
| Event on non-working day             | Skip; mark Core event PROCESSED; no record       |
| Duplicate event for same student-date| Idempotent; existing record not overwritten       |
| `core-attendance` unavailable        | Job retries on next cycle; logs WARN             |
| Student not in any active section    | Skip event; mark PROCESSED; log WARN             |

---

## 18. Logging and Audit

| Action                    | Audit Code                     | Details                          |
|---------------------------|--------------------------------|----------------------------------|
| Record created (PRESENT)  | `ATTENDANCE_RECORD_CREATED`    | student_id, date, status         |
| Record created (ABSENT)   | `ATTENDANCE_RECORD_ABSENT`     | student_id, date                 |
| Record created (LATE)     | `ATTENDANCE_RECORD_LATE`       | student_id, date, arrival_time   |
| Record overridden         | `ATTENDANCE_RECORD_OVERRIDDEN` | record_id, old_status, new_status|

---

## 19. Flyway Migrations

```
V117__create_school_daily_attendance_table.sql
```

---

## 20. Testing Strategy

| Test Type       | Scope                                                                        |
|-----------------|------------------------------------------------------------------------------|
| Unit — Service  | Process event: PRESENT, LATE, ON_LEAVE, non-working day skip                 |
| Unit — Service  | Mark-absent job: identifies students without records; skips leave students   |
| Unit — Service  | Idempotency: duplicate event does not create duplicate record                |
| Unit — Service  | Override: status change logged to audit                                      |
| Repository test | `findBySectionAndDate`, `findByStudentAndDateRange`                          |
| Integration     | Event in Core → processing job → DailyAttendanceRecord created               |
| Integration     | Holiday date → event processed → no record created                          |

---

## 21. Implementation Roadmap

### Task 1: Entity and migration
- `DailyAttendanceRecord`, `DailyAttendanceStatus`; Flyway V117

### Task 2: Service — event processing
- `processAttendanceEvent()`: resolve student, calendar check, rules, create record
- Call `AttendanceService.markAsProcessed()`

### Task 3: Service — mark absent job
- `runMarkAbsentJob()`: get active school's enrolled students, find missing records, mark ABSENT
- Check approved leave before marking ABSENT

### Task 4: Schedulers
- `AttendanceProcessingJob` (`@Scheduled`) — polls pending events
- `MarkAbsentJob` (`@Scheduled`) — daily absent marking

### Task 5: Override and query
- `overrideAttendance()`, `getSectionAttendanceForDate()`, `getStudentAttendance()`

### Task 6: Controller and DTOs

### Task 7: Notification integration
- Fire `SCHOOL_STUDENT_ABSENT` via `NotificationService.send()`

### Task 8: Audit integration

---

## 22. Acceptance Criteria

- [ ] Events on non-working days are skipped and marked PROCESSED with no record
- [ ] Students arriving after the LATE threshold are marked LATE, not ABSENT
- [ ] Students on approved leave are marked ON_LEAVE, not ABSENT
- [ ] Only one `DailyAttendanceRecord` per student per date
- [ ] Absent notification is sent to guardian when a student is marked ABSENT
- [ ] Overriding a record logs old and new status to the audit trail
- [ ] Mark-absent job only runs on working days
- [ ] Event processing is idempotent

---

## 23. Out of Scope

- Subject-level (period-level) attendance (V1: day-level only)
- Real-time WebSocket push of attendance updates
- Manual bulk attendance entry via spreadsheet upload
- Attendance finalization / locking per day
