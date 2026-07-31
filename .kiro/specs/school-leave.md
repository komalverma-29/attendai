# Specification: school-leave

## 1. Overview

`school-leave` manages leave applications for students and teachers within a school. It provides a workflow for requesting leave, reviewing it, approving or rejecting it, and reflecting the outcome in the attendance system.

When a student's leave is approved, the `DailyAttendanceRecord` for the leave period is set to `ON_LEAVE` status, which excludes those days from absence calculations.

---

## 2. Scope and Objectives

**In scope:**
- Student leave application and management
- Teacher leave application and management
- Leave type definitions (SICK, CASUAL, MEDICAL, FAMILY, OTHER)
- Approval workflow: PENDING → APPROVED / REJECTED
- Applying approved leave to daily attendance records
- Leave balance tracking per academic year (optional, configurable)
- Leave history queries

**Out of scope:**
- Payroll integration for teacher leave
- Leave encashment
- Leave carry-forward across academic years
- Long-term absence tracking beyond marking ON_LEAVE

---

## 3. Functional Requirements

### FR-LEAVE-01: Create Leave Application
A student (or admin on behalf of a student), or a teacher, submits a leave application specifying leave type, start date, end date, and reason.

### FR-LEAVE-02: Get Leave Application by ID
Retrieve a single leave application.

### FR-LEAVE-03: List Leave Applications
Return a paginated list of leave applications filterable by school, student/teacher, status, leave type, and date range.

### FR-LEAVE-04: Approve Leave Application
An administrator approves a PENDING leave application. On approval:
1. For each working day in the leave period, set or update the `DailyAttendanceRecord` status to `ON_LEAVE`.
2. Set leave application status to `APPROVED`.
3. Write audit log.

### FR-LEAVE-05: Reject Leave Application
An administrator rejects the leave with a reason. No attendance records are modified.

### FR-LEAVE-06: Cancel Leave Application
The applicant cancels their own PENDING application. Already-approved leave can be cancelled by admin with a different workflow (FR-LEAVE-07).

### FR-LEAVE-07: Revoke Approved Leave
An administrator revokes a previously approved leave (e.g., student returns early). The attendance records for remaining days are reset to their computed status (re-processed from Core events or marked ABSENT if no event).

### FR-LEAVE-08: Get Leave Balance for Student
Return the number of leave days used and remaining for each leave type in the current academic year (if leave balance tracking is enabled in school settings).

---

## 4. Business Rules

- BR-LEAVE-01: Leave start date cannot be in the past by more than 7 days (configurable). Future dates are allowed.
- BR-LEAVE-02: Leave end date must be >= start date.
- BR-LEAVE-03: Leave is applied only to working days in the date range. Weekend and holiday dates within the leave period are ignored.
- BR-LEAVE-04: Only one PENDING or APPROVED leave application per student per overlapping date range. Overlapping leaves are rejected.
- BR-LEAVE-05: Approving leave for a COMPLETED academic year is rejected.
- BR-LEAVE-06: If leave balance tracking is enabled, approval checks if the student has sufficient balance for the leave type. Insufficient balance rejects with a warning (configurable: hard reject or soft warning).
- BR-LEAVE-07: A student's approved leave for a date overrides any ABSENT status set by the mark-absent job.

---

## 5. Leave Application Status State Machine

```
[Submitted] → PENDING
PENDING     → APPROVED     (admin approves → attendance records updated)
PENDING     → REJECTED     (admin rejects)
PENDING     → CANCELLED    (applicant cancels)
APPROVED    → REVOKED      (admin revokes → attendance records reset)
REVOKED     → [immutable]
REJECTED    → [immutable]
CANCELLED   → [immutable]
```

---

## 6. Domain Model

### LeaveType Enum
- `SICK`
- `CASUAL`
- `MEDICAL`
- `FAMILY`
- `OTHER`

### LeaveApplicantType Enum
- `STUDENT`
- `TEACHER`

### LeaveApplication Entity

| Field           | Type                | Description                                            |
|-----------------|---------------------|--------------------------------------------------------|
| id              | Long                | Surrogate PK                                           |
| schoolId        | Long                | FK → school_schools(id), NOT NULL                      |
| academicYearId  | Long                | FK → school_academic_years(id), NOT NULL               |
| applicantType   | LeaveApplicantType  | STUDENT or TEACHER                                     |
| studentId       | Long                | FK → school_students(id), nullable if teacher          |
| teacherId       | Long                | FK → school_teachers(id), nullable if student          |
| leaveType       | LeaveType           | Enum: SICK, CASUAL, MEDICAL, FAMILY, OTHER             |
| startDate       | LocalDate           | NOT NULL                                               |
| endDate         | LocalDate           | NOT NULL, >= startDate                                 |
| totalDays       | int                 | Calendar days requested, NOT NULL                      |
| workingDays     | int                 | Working days within the range (computed on approval)   |
| reason          | String              | NOT NULL, max 1000                                     |
| evidenceFileId  | Long                | Optional, FK → files(id)                               |
| status          | LeaveStatus         | Enum: PENDING, APPROVED, REJECTED, CANCELLED, REVOKED  |
| approvedById    | Long                | userId who approved, nullable                          |
| approvedAt      | LocalDateTime       | Nullable                                               |
| rejectionReason | String              | Nullable, max 1000                                     |
| revokedById     | Long                | userId who revoked, nullable                           |
| revokedAt       | LocalDateTime       | Nullable                                               |
| createdAt       | LocalDateTime       | Audit                                                  |
| updatedAt       | LocalDateTime       | Audit                                                  |
| createdBy       | Long                | Audit                                                  |
| updatedBy       | Long                | Audit                                                  |

---

## 7. Data Model

### Table: `school_leave_applications`

```sql
CREATE TABLE school_leave_applications (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    applicant_type   VARCHAR(10)      NOT NULL,
    student_id       BIGINT UNSIGNED  NULL,
    teacher_id       BIGINT UNSIGNED  NULL,
    leave_type       VARCHAR(20)      NOT NULL,
    start_date       DATE             NOT NULL,
    end_date         DATE             NOT NULL,
    total_days       INT              NOT NULL,
    working_days     INT              NULL,
    reason           VARCHAR(1000)    NOT NULL,
    evidence_file_id BIGINT UNSIGNED  NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    approved_by_id   BIGINT UNSIGNED  NULL,
    approved_at      DATETIME         NULL,
    rejection_reason VARCHAR(1000)    NULL,
    revoked_by_id    BIGINT UNSIGNED  NULL,
    revoked_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_leave_school   FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_leave_year     FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_leave_student  FOREIGN KEY (student_id)       REFERENCES school_students(id),
    CONSTRAINT fk_leave_teacher  FOREIGN KEY (teacher_id)       REFERENCES school_teachers(id),
    INDEX idx_leave_school (school_id),
    INDEX idx_leave_student (student_id),
    INDEX idx_leave_teacher (teacher_id),
    INDEX idx_leave_status (status),
    INDEX idx_leave_dates (start_date, end_date)
);
```

---

## 8. Package Organization

```
com.attendai.school.leave
├── entity
│   ├── LeaveApplication.java
│   ├── LeaveType.java
│   ├── LeaveStatus.java
│   └── LeaveApplicantType.java
├── repository
│   └── LeaveApplicationRepository.java
├── service
│   ├── LeaveApplicationService.java
│   └── LeaveApplicationServiceImpl.java
├── controller
│   └── LeaveApplicationController.java
├── dto
│   ├── CreateLeaveApplicationRequest.java
│   ├── ReviewLeaveRequest.java
│   ├── LeaveApplicationResponse.java
│   └── LeaveApplicationSummaryResponse.java
├── mapper
│   └── LeaveApplicationMapper.java
└── exception
    └── LeaveApplicationNotFoundException.java
```

---

## 9. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/leave`

### POST — Submit Leave Application

**Permission:** `SCHOOL_LEAVE_REQUEST` (teachers/students) or `SCHOOL_LEAVE_MANAGE` (admin on behalf)

**Request:**
```json
{
  "applicantType": "STUDENT",
  "studentId": 5,
  "leaveType": "SICK",
  "startDate": "2025-09-10",
  "endDate": "2025-09-12",
  "reason": "Fever and cold",
  "evidenceFileId": null
}
```

**Response 201:** `LeaveApplicationResponse`

---

### GET /{id}

**Permission:** `SCHOOL_LEAVE_READ`

**Response 200:** `LeaveApplicationResponse`

---

### GET — List Leave Applications

**Permission:** `SCHOOL_LEAVE_READ`

**Query params:** `studentId`, `teacherId`, `status`, `leaveType`, `fromDate`, `toDate`, `page`, `size`

**Response 200:** Paginated `LeaveApplicationSummaryResponse`

---

### PATCH /{id}/approve

**Permission:** `SCHOOL_LEAVE_MANAGE`

**Request:** `{ "remarks": "Approved with medical certificate" }`

**Response 200:** `LeaveApplicationResponse`

---

### PATCH /{id}/reject

**Permission:** `SCHOOL_LEAVE_MANAGE`

**Request:** `{ "rejectionReason": "Insufficient leave balance" }`

**Response 200:** `LeaveApplicationResponse`

---

### PATCH /{id}/cancel

**Permission:** Own application or `SCHOOL_LEAVE_MANAGE`

**Response 200:** `LeaveApplicationResponse`

---

### PATCH /{id}/revoke

**Permission:** `SCHOOL_LEAVE_MANAGE`

**Response 200:** `LeaveApplicationResponse`

---

## 10. Authorization

| Operation                   | Permission               |
|-----------------------------|--------------------------|
| Submit leave                | `SCHOOL_LEAVE_REQUEST`   |
| Read leave                  | `SCHOOL_LEAVE_READ`      |
| Approve/reject/revoke       | `SCHOOL_LEAVE_MANAGE`    |
| Cancel own application      | Own user (authenticated) |

---

## 11. Integration Points

| Module                     | Integration                                                         |
|----------------------------|---------------------------------------------------------------------|
| `school-daily-attendance`  | Sets `ON_LEAVE` status on `DailyAttendanceRecord` on approval       |
| `school-academic-calendar` | `getWorkingDates()` to compute working days within leave range      |
| `school-academic-year`     | Validates active year; rejects COMPLETED year leaves                |
| `school-student`           | Validates studentId on create                                       |
| `school-teacher`           | Validates teacherId on create                                       |
| `core-file`                | Optional evidence file reference                                    |
| `core-notification`        | Notify applicant on approval/rejection                              |
| `core-audit`               | Audit all state transitions                                         |

---

## 12. Error Handling

| Scenario                         | Exception                          | HTTP |
|----------------------------------|------------------------------------|------|
| Leave not found                  | `LeaveApplicationNotFoundException`| 404  |
| Overlapping leave application    | `ResourceAlreadyExistsException`   | 409  |
| Leave for COMPLETED year         | `ValidationException`              | 400  |
| Insufficient leave balance       | `ValidationException`              | 400  |
| endDate before startDate         | `ValidationException`              | 400  |

---

## 13. Flyway Migrations

```
V121__create_school_leave_applications_table.sql
```

---

## 14. Acceptance Criteria

- [ ] Approved leave sets `DailyAttendanceRecord` to `ON_LEAVE` for each working day in the range
- [ ] `ON_LEAVE` days are not counted as absent in attendance percentage calculation
- [ ] Overlapping leave applications for the same student are rejected
- [ ] Leave revocation resets attendance records to computed status
- [ ] Weekend and holiday days within leave period do not get `ON_LEAVE` records
- [ ] All state transitions produce audit log entries

---

## 15. Out of Scope

- Leave balance/quota management (V1: tracking only, no hard enforcement)
- Payroll integration for teacher leave
- Leave carry-forward across academic years
