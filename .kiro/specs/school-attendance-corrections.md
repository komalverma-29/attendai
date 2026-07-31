# Specification: school-attendance-corrections

## 1. Overview

`school-attendance-corrections` provides the workflow for requesting, reviewing, and applying corrections to existing daily attendance records. A correction allows an administrator or authorized teacher to change a student's attendance status for a past date with a documented reason and an approval workflow.

Corrections must be traceable. Every correction maintains a full audit trail: who requested it, who approved or rejected it, what the original status was, and what it was changed to.

---

## 2. Scope and Objectives

**In scope:**
- Submitting a correction request for a student's attendance record
- Approval workflow: PENDING → APPROVED / REJECTED
- Applying the approved correction to the `DailyAttendanceRecord`
- Linking corrections to the underlying Core attendance event (if applicable)
- Audit trail for all correction operations
- Querying correction requests by section, student, date range, and status

**Out of scope:**
- Leave management (belongs in `school-leave`)
- Bulk correction import
- Corrections for future dates

---

## 3. Functional Requirements

### FR-CORR-01: Submit Correction Request
A teacher or administrator submits a correction for a student's attendance on a specific date. The request captures: student ID, date, requested status, reason, and any supporting evidence reference (file ID).

### FR-CORR-02: List Correction Requests
Return a paginated list of correction requests filterable by school, section, student, status, and date range.

### FR-CORR-03: Get Correction Request by ID
Retrieve a single correction request.

### FR-CORR-04: Approve Correction
An administrator approves a PENDING correction request. On approval:
1. The `DailyAttendanceRecord` status is updated to the requested status.
2. The correction request status is set to `APPROVED`.
3. An audit log entry is written.

### FR-CORR-05: Reject Correction
An administrator rejects a PENDING correction request with a rejection reason. The `DailyAttendanceRecord` is not modified.

### FR-CORR-06: Cancel Correction Request
The requester cancels their own PENDING request before it is reviewed.

---

## 4. Business Rules

- BR-CORR-01: A correction can only be requested for past or current dates within the active academic year. Future dates are rejected.
- BR-CORR-02: A correction cannot be submitted for a date that is not a working day.
- BR-CORR-03: A correction cannot change a status to `ON_LEAVE` via corrections — leave must be processed through `school-leave`.
- BR-CORR-04: Only one PENDING correction is allowed per student per date. A second request is rejected if one is already pending.
- BR-CORR-05: Corrections for COMPLETED academic years are rejected.
- BR-CORR-06: The requester and approver cannot be the same person for the same correction (four-eyes principle, configurable).
- BR-CORR-07: Approving or rejecting a non-PENDING correction is rejected.

---

## 5. Correction Request Status State Machine

```
[Submitted] → PENDING
PENDING     → APPROVED   (admin approves → DailyAttendanceRecord updated)
PENDING     → REJECTED   (admin rejects)
PENDING     → CANCELLED  (requester cancels)
APPROVED    → [immutable]
REJECTED    → [immutable]
CANCELLED   → [immutable]
```

---

## 6. Domain Model

### AttendanceCorrectionRequest Entity

| Field              | Type              | Description                                                  |
|--------------------|-------------------|--------------------------------------------------------------|
| id                 | Long              | Surrogate PK                                                 |
| schoolId           | Long              | FK → school_schools(id), NOT NULL                            |
| academicYearId     | Long              | FK → school_academic_years(id), NOT NULL                     |
| studentId          | Long              | FK → school_students(id), NOT NULL                           |
| attendanceRecordId | Long              | FK → school_daily_attendance(id), NOT NULL                   |
| attendanceDate     | LocalDate         | Denormalized for query convenience, NOT NULL                 |
| originalStatus     | DailyAttendanceStatus | Status before correction, NOT NULL                     |
| requestedStatus    | DailyAttendanceStatus | Status being requested, NOT NULL                       |
| reason             | String            | Required reason for correction, max 1000                     |
| evidenceFileId     | Long              | Optional file reference via core-file                        |
| status             | CorrectionStatus  | Enum: PENDING, APPROVED, REJECTED, CANCELLED                 |
| requestedById      | Long              | userId of the requester, NOT NULL                            |
| reviewedById       | Long              | userId of the reviewer, nullable                             |
| reviewedAt         | LocalDateTime     | When reviewed, nullable                                      |
| rejectionReason    | String            | Reason for rejection, nullable, max 1000                     |
| createdAt          | LocalDateTime     | Audit                                                        |
| updatedAt          | LocalDateTime     | Audit                                                        |
| createdBy          | Long              | Audit                                                        |
| updatedBy          | Long              | Audit                                                        |

---

## 7. Data Model

### Table: `school_attendance_corrections`

```sql
CREATE TABLE school_attendance_corrections (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id            BIGINT UNSIGNED  NOT NULL,
    academic_year_id     BIGINT UNSIGNED  NOT NULL,
    student_id           BIGINT UNSIGNED  NOT NULL,
    attendance_record_id BIGINT UNSIGNED  NOT NULL,
    attendance_date      DATE             NOT NULL,
    original_status      VARCHAR(20)      NOT NULL,
    requested_status     VARCHAR(20)      NOT NULL,
    reason               VARCHAR(1000)    NOT NULL,
    evidence_file_id     BIGINT UNSIGNED  NULL,
    status               VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    requested_by_id      BIGINT UNSIGNED  NOT NULL,
    reviewed_by_id       BIGINT UNSIGNED  NULL,
    reviewed_at          DATETIME         NULL,
    rejection_reason     VARCHAR(1000)    NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_corrections_school  FOREIGN KEY (school_id)            REFERENCES school_schools(id),
    CONSTRAINT fk_corrections_year    FOREIGN KEY (academic_year_id)     REFERENCES school_academic_years(id),
    CONSTRAINT fk_corrections_student FOREIGN KEY (student_id)           REFERENCES school_students(id),
    CONSTRAINT fk_corrections_record  FOREIGN KEY (attendance_record_id) REFERENCES school_daily_attendance(id),
    INDEX idx_corrections_school (school_id),
    INDEX idx_corrections_student (student_id),
    INDEX idx_corrections_status (status),
    INDEX idx_corrections_date (attendance_date)
);
```

---

## 8. Package Organization

```
com.attendai.school.attendancecorrections
├── entity
│   ├── AttendanceCorrectionRequest.java
│   └── CorrectionStatus.java
├── repository
│   └── AttendanceCorrectionRepository.java
├── service
│   ├── AttendanceCorrectionService.java
│   └── AttendanceCorrectionServiceImpl.java
├── controller
│   └── AttendanceCorrectionController.java
├── dto
│   ├── CreateCorrectionRequest.java
│   ├── ReviewCorrectionRequest.java
│   ├── CorrectionRequestResponse.java
│   └── CorrectionSummaryResponse.java
├── mapper
│   └── AttendanceCorrectionMapper.java
└── exception
    └── CorrectionRequestNotFoundException.java
```

---

## 9. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/attendance/corrections`

### POST — Submit Correction

**Permission:** `SCHOOL_ATTENDANCE_CORRECTION_REQUEST`

**Request:**
```json
{
  "studentId": 5,
  "attendanceDate": "2025-09-01",
  "requestedStatus": "PRESENT",
  "reason": "Student was present but station was offline",
  "evidenceFileId": null
}
```

**Response 201:** `CorrectionRequestResponse`

---

### GET — List Corrections

**Permission:** `SCHOOL_ATTENDANCE_CORRECTION_READ`

**Query params:** `studentId`, `sectionId`, `status`, `fromDate`, `toDate`, `page`, `size`

**Response 200:** Paginated `CorrectionSummaryResponse`

---

### GET /{id}

**Permission:** `SCHOOL_ATTENDANCE_CORRECTION_READ`

**Response 200:** `CorrectionRequestResponse`

---

### PATCH /{id}/approve

**Permission:** `SCHOOL_ATTENDANCE_CORRECTION_APPROVE`

**Request:** `{ "remarks": "Verified via CCTV" }`

**Response 200:** `CorrectionRequestResponse` — `status = APPROVED`

---

### PATCH /{id}/reject

**Permission:** `SCHOOL_ATTENDANCE_CORRECTION_APPROVE`

**Request:** `{ "rejectionReason": "No supporting evidence provided" }`

**Response 200:** `CorrectionRequestResponse` — `status = REJECTED`

---

### PATCH /{id}/cancel

**Permission:** Own request only

**Response 200:** `CorrectionRequestResponse` — `status = CANCELLED`

---

## 10. Validation Rules

| Field           | Rule                                                            |
|-----------------|-----------------------------------------------------------------|
| studentId       | Not null, active student in this school                         |
| attendanceDate  | Not null, past or today, working day, within active year        |
| requestedStatus | Not null, not `ON_LEAVE` (use school-leave for that)           |
| reason          | Not blank, max 1000                                             |
| evidenceFileId  | Optional, must reference valid file if provided                 |

---

## 11. Authorization

| Operation              | Permission                              |
|------------------------|-----------------------------------------|
| Submit correction      | `SCHOOL_ATTENDANCE_CORRECTION_REQUEST`  |
| Read correction        | `SCHOOL_ATTENDANCE_CORRECTION_READ`     |
| Approve/reject         | `SCHOOL_ATTENDANCE_CORRECTION_APPROVE`  |
| Cancel own request     | Authenticated user (own request only)   |

---

## 12. Integration Points

| Module                     | Integration                                                     |
|----------------------------|-----------------------------------------------------------------|
| `school-daily-attendance`  | Updates `DailyAttendanceRecord` on approval                     |
| `school-academic-calendar` | Validates correction date is a working day                      |
| `school-academic-year`     | Rejects corrections for COMPLETED years                         |
| `core-file`                | Optional evidence file reference                                |
| `core-audit`               | Audit events for all correction state transitions               |
| `core-notification`        | Optional: notify requester when correction is approved/rejected |

---

## 13. Error Handling

| Scenario                              | Exception                              | HTTP |
|---------------------------------------|----------------------------------------|------|
| Correction not found                  | `CorrectionRequestNotFoundException`  | 404  |
| Duplicate pending correction          | `ResourceAlreadyExistsException`       | 409  |
| Correction to ON_LEAVE status         | `ValidationException`                  | 400  |
| Future date                           | `ValidationException`                  | 400  |
| Non-working day date                  | `ValidationException`                  | 400  |
| Reviewing non-PENDING correction      | `ValidationException`                  | 400  |
| COMPLETED year correction             | `ValidationException`                  | 400  |

---

## 14. Logging and Audit

| Action               | Audit Code                       | Details                           |
|----------------------|----------------------------------|-----------------------------------|
| Correction submitted | `CORRECTION_SUBMITTED`           | correction_id, student_id, date   |
| Correction approved  | `CORRECTION_APPROVED`            | correction_id, old_status, new_status |
| Correction rejected  | `CORRECTION_REJECTED`            | correction_id, reason             |
| Correction cancelled | `CORRECTION_CANCELLED`           | correction_id                     |

---

## 15. Flyway Migrations

```
V120__create_school_attendance_corrections_table.sql
```

---

## 16. Acceptance Criteria

- [ ] Correction to `ON_LEAVE` status is rejected
- [ ] Only one PENDING correction per student per date
- [ ] Approving a correction updates the `DailyAttendanceRecord` status
- [ ] Self-approval is blocked (approver ≠ requester) when four-eyes is enabled
- [ ] Corrections for COMPLETED academic years are rejected
- [ ] Full audit trail: original status, requested status, reviewer, timestamps
- [ ] All state transitions produce audit log entries

---

## 17. Out of Scope

- Bulk correction via file import
- Leave-based attendance changes (school-leave)
- Corrections to Core attendance events (handled by `core-attendance` correction API)
