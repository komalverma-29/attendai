# Specification: school-dashboard

## 1. Overview

`school-dashboard` provides aggregated, real-time attendance statistics for the school administrator's and teacher's operational view. It is a read-only module that queries `school_daily_attendance` and related tables to produce summary metrics for daily operations and trend monitoring.

The dashboard is not a reporting module — it shows today's live state and short-term trends. Deep historical analysis belongs in `school-attendance-reports`.

---

## 2. Scope and Objectives

**In scope:**
- Today's school-wide attendance overview (present/absent/late counts)
- Today's section-level attendance summary
- Attendance trend for the last 30 days (school-wide)
- Low-attendance alerts: students below minimum threshold
- Pending leave applications count
- Pending correction requests count
- Teacher attendance statistics (if teacher attendance is tracked)

**Out of scope:**
- Historical report generation (belongs in `school-attendance-reports`)
- Data export
- Real-time WebSocket push

---

## 3. Functional Requirements

### FR-DASH-01: School Overview for Today
Return the school-wide attendance summary for today:
- Total active students
- Present, Late, Absent, On Leave counts
- Overall attendance percentage for today
- Whether today is a working day

### FR-DASH-02: Section-Wise Summary for Today
Return today's attendance summary for every active section in the school, ordered by class and section name.

### FR-DASH-03: Attendance Trend
Return daily attendance percentages for the last 30 working days for the school or a specific section.

### FR-DASH-04: Low Attendance Alerts
Return the count and list of students currently below the minimum attendance threshold for the active academic year.

### FR-DASH-05: Pending Action Counts
Return:
- Count of PENDING correction requests
- Count of PENDING leave applications
- Count of students with N+ consecutive absences (from attendance rules)

### FR-DASH-06: Section Dashboard (Teacher View)
Return today's attendance and recent trend for a specific section. Used by teachers to see their class attendance at a glance.

---

## 4. Non-Functional Requirements

- School overview must respond within 300ms.
- Section-wise summary must respond within 500ms for up to 50 sections.
- Dashboard data is computed on-demand. No separate aggregation tables in V1.
- All dashboard endpoints cache responses for 2 minutes (configurable TTL) to reduce DB load during frequent polling.

---

## 5. Business Rules

- BR-DASH-01: If today is not a working day, the school overview returns `{ isWorkingDay: false }` and zero counts.
- BR-DASH-02: Sections with no attendance records for today show as "0 processed / N expected".
- BR-DASH-03: Trend data covers only working days; non-working days are omitted from the series.
- BR-DASH-04: Low attendance alerts use the current attendance percentage (from enrollment date to today).

---

## 6. Package Organization

```
com.attendai.school.dashboard
├── service
│   ├── DashboardService.java
│   └── DashboardServiceImpl.java
├── controller
│   └── DashboardController.java
└── dto
    ├── SchoolOverviewResponse.java
    ├── SectionDailySummaryResponse.java
    ├── AttendanceTrendResponse.java
    ├── LowAttendanceAlertResponse.java
    └── PendingActionsResponse.java
```

No new database tables.

---

## 7. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/dashboard`

### GET /overview — School Overview for Today

**Permission:** `SCHOOL_DASHBOARD_READ`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "date": "2025-09-15",
    "isWorkingDay": true,
    "academicYearId": 2,
    "totalStudents": 450,
    "present": 380,
    "late": 25,
    "absent": 30,
    "onLeave": 15,
    "attendancePercentage": 89.89,
    "pendingActions": {
      "correctionRequests": 3,
      "leaveApplications": 7,
      "consecutiveAbsenceAlerts": 5
    }
  }
}
```

---

### GET /sections/summary — Section-Wise Summary for Today

**Permission:** `SCHOOL_DASHBOARD_READ`

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "sectionId": 10,
      "className": "Grade 5",
      "sectionName": "A",
      "totalStudents": 40,
      "present": 35,
      "late": 2,
      "absent": 2,
      "onLeave": 1,
      "attendancePercentage": 92.50
    }
  ]
}
```

---

### GET /trend — Attendance Trend (Last 30 Working Days)

**Permission:** `SCHOOL_DASHBOARD_READ`

**Query params:** `sectionId` (optional)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "sectionId": null,
    "trend": [
      { "date": "2025-09-01", "percentage": 91.20 },
      { "date": "2025-09-02", "percentage": 88.50 }
    ]
  }
}
```

---

### GET /low-attendance — Low Attendance Alerts

**Permission:** `SCHOOL_DASHBOARD_READ`

**Query params:** `sectionId` (optional), `page`, `size`

**Response 200:** Paginated `LowAttendanceAlertResponse`
```json
{
  "success": true,
  "data": [
    {
      "studentId": 6,
      "fullName": "Priya Sharma",
      "sectionName": "Grade 5 - A",
      "currentPercentage": 68.50,
      "threshold": 75.00,
      "shortfallDays": 8
    }
  ]
}
```

---

### GET /sections/{sectionId} — Section Dashboard

**Permission:** `SCHOOL_DASHBOARD_READ` or `SCHOOL_TEACHER_DASHBOARD_READ`

**Response 200:** Today's section overview + 7-day trend

---

## 8. Authorization

| Operation               | Permission                       |
|-------------------------|----------------------------------|
| School-wide dashboard   | `SCHOOL_DASHBOARD_READ`          |
| Section dashboard       | `SCHOOL_DASHBOARD_READ` or `SCHOOL_TEACHER_DASHBOARD_READ` |

Teachers with `SCHOOL_TEACHER_DASHBOARD_READ` can only access sections they are assigned to.

---

## 9. Integration Points

| Module                     | Integration                                             |
|----------------------------|---------------------------------------------------------|
| `school-daily-attendance`  | Primary data source                                     |
| `school-academic-calendar` | `isWorkingDay()` for today's check; working day trend   |
| `school-attendance-rules`  | Threshold for low-attendance alerts                     |
| `school-leave`             | Pending leave application count                         |
| `school-attendance-corrections` | Pending correction count                          |
| `school-section`           | Section list for section-wise summary                   |
| `school-academic-year`     | Active academic year scope                              |

---

## 10. Performance Considerations

- All dashboard queries are bounded to today or last 30 working days — they are always indexed and time-bounded.
- Response caching (2-minute TTL per school) is implemented at the service layer using `@Cacheable` or a `ConcurrentHashMap` with timestamp-based expiry.
- Cache is invalidated when attendance records for the school change (post-processing job completion).

---

## 11. Flyway Migrations

None. This module has no new tables.

---

## 12. Testing Strategy

| Test Type      | Scope                                                              |
|----------------|--------------------------------------------------------------------|
| Unit — Service | `getSchoolOverview`: today = holiday returns empty stats           |
| Unit — Service | Section summary: correct aggregation                               |
| Unit — Service | Trend: working days only, sorted ascending                         |
| Unit — Service | Low attendance: correctly identifies students below threshold      |
| Controller test| All endpoints, permission checks, response structure               |

---

## 13. Acceptance Criteria

- [ ] Overview returns `isWorkingDay: false` with zero attendance counts on holidays
- [ ] Section summary correctly aggregates present/absent/late/onLeave per section
- [ ] Trend series contains only working days
- [ ] Low attendance alerts use the `minAttendancePercentage` from attendance rules
- [ ] `SCHOOL_TEACHER_DASHBOARD_READ` grants access to assigned sections only

---

## 14. Out of Scope

- Historical reporting (school-attendance-reports)
- Real-time WebSocket push
- Data export
- Fee, exam, or transport summaries
