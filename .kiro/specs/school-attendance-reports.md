# Specification: school-attendance-reports

## 1. Overview

`school-attendance-reports` provides all attendance reporting and analytics capabilities for the school. Reports are computed from `DailyAttendanceRecord` data and enriched with academic calendar working-day counts and attendance rule thresholds.

Reports are read-only. They do not modify any attendance data.

---

## 2. Scope and Objectives

**In scope:**
- Student attendance summary report (% attendance for a student over a period)
- Section attendance summary report (% attendance per student in a section)
- Attendance shortage report (students below minimum attendance threshold)
- Daily attendance register (full day-by-day record for a section)
- School-wide attendance overview
- Consecutive absence report (students with N+ consecutive absences)

**Out of scope:**
- Real-time attendance dashboards (belongs in `school-dashboard`)
- Attendance data modification (belongs in `school-daily-attendance` and `school-attendance-corrections`)
- Report export to PDF/Excel (V1: JSON API only; export is a future enhancement)

---

## 3. Functional Requirements

### FR-RPT-01: Student Attendance Summary
Given a student ID and date range (or full academic year), return:
- Total working days in the range
- Days present, absent, late, on_leave
- Attendance percentage (`(present + late) / total_working_days * 100`)
- Whether the student is below the minimum attendance threshold

### FR-RPT-02: Section Attendance Summary
Given a section ID and date range, return an attendance summary for every student in the section, ordered by roll number.

### FR-RPT-03: Attendance Shortage Report
Return all students in a school (or section) whose attendance percentage falls below the configured minimum threshold for the academic year. Includes the current percentage and the shortfall.

### FR-RPT-04: Daily Attendance Register
Given a section and a date range, return a matrix-style register:
- Rows: students (ordered by roll number)
- Columns: dates (working days only)
- Cells: attendance status (P, A, L, OL)

### FR-RPT-05: Consecutive Absence Report
Return all students who have been absent for N or more consecutive working days (N from attendance rules). Includes the start date of the consecutive absence streak.

### FR-RPT-06: School Attendance Overview
Return aggregate statistics for a school on a given date or date range:
- Total students
- Present count, absent count, late count, on-leave count
- Overall attendance percentage

---

## 4. Non-Functional Requirements

- All reports are computed on-demand. No pre-aggregated summary tables in V1.
- Student attendance summary must respond within 500ms for up to 1 academic year of data.
- Section summary report for 60 students and 1 month must respond within 1 second.
- Daily register for 60 students × 30 days must respond within 1 second.
- Reports are always scoped to a school — cross-school queries are not permitted.

---

## 5. Business Rules

- BR-RPT-01: Attendance percentage formula: `(presentDays + lateDays) / workingDays * 100`, rounded to 2 decimal places.
- BR-RPT-02: `ON_LEAVE` days do not count as absent in percentage calculation. They are excluded from both numerator and denominator.
- BR-RPT-03: Working days are determined by `AcademicCalendarService.getWorkingDayCount()`.
- BR-RPT-04: The shortage threshold is loaded from `AttendanceRulesService.getMinAttendancePercentage()`.
- BR-RPT-05: Reports are always filtered to dates within the academic year's date range.
- BR-RPT-06: Students with TRANSFERRED or GRADUATED status are included in historical reports but excluded from current-period shortage reports.

---

## 6. Package Organization

```
com.attendai.school.attendancereports
├── service
│   ├── AttendanceReportService.java
│   └── AttendanceReportServiceImpl.java
├── controller
│   └── AttendanceReportController.java
├── dto
│   ├── StudentAttendanceSummaryResponse.java
│   ├── SectionAttendanceSummaryResponse.java
│   ├── AttendanceShortageResponse.java
│   ├── DailyAttendanceRegisterResponse.java
│   ├── ConsecutiveAbsenceResponse.java
│   └── SchoolAttendanceOverviewResponse.java
└── (no entity — reads from school_daily_attendance)
```

No new database tables. All reports query `school_daily_attendance` directly.

---

## 7. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/reports/attendance`

### GET /students/{studentId}/summary

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `academicYearId`, `fromDate` (optional), `toDate` (optional)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "studentId": 5,
    "fullName": "Arjun Patel",
    "admissionNumber": "ADM-2025-001",
    "academicYearId": 2,
    "period": { "from": "2025-06-01", "to": "2025-09-30" },
    "workingDays": 90,
    "presentDays": 72,
    "lateDays": 5,
    "absentDays": 8,
    "onLeaveDays": 5,
    "attendancePercentage": 85.56,
    "minimumRequired": 75.00,
    "belowThreshold": false
  }
}
```

---

### GET /sections/{sectionId}/summary

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `academicYearId`, `fromDate`, `toDate`

**Response 200:** List of `StudentAttendanceSummaryResponse` ordered by roll number

---

### GET /shortage

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `academicYearId`, `sectionId` (optional)

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "studentId": 6,
      "fullName": "Priya Sharma",
      "sectionName": "Grade 5 - A",
      "attendancePercentage": 68.50,
      "minimumRequired": 75.00,
      "shortfallPercentage": 6.50,
      "daysRequired": 8
    }
  ]
}
```

---

### GET /sections/{sectionId}/register

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `academicYearId`, `fromDate`, `toDate`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "sectionId": 10,
    "sectionName": "Grade 5 - A",
    "workingDates": ["2025-09-01", "2025-09-02", "2025-09-03"],
    "students": [
      {
        "studentId": 5,
        "rollNumber": "01",
        "fullName": "Arjun Patel",
        "attendance": ["P", "P", "A"]
      }
    ]
  }
}
```

Legend: P = Present, L = Late, A = Absent, OL = On Leave

---

### GET /consecutive-absences

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `academicYearId`, `sectionId` (optional), `minConsecutiveDays` (optional, defaults to rule threshold)

**Response 200:** List of `ConsecutiveAbsenceResponse`

---

### GET /overview

**Permission:** `SCHOOL_ATTENDANCE_REPORT_READ`

**Query params:** `date` OR `fromDate`+`toDate`

**Response 200:** `SchoolAttendanceOverviewResponse`

---

## 8. Authorization

| Operation         | Permission                         |
|-------------------|------------------------------------|
| All report reads  | `SCHOOL_ATTENDANCE_REPORT_READ`    |

---

## 9. Integration Points

| Module                     | Integration                                                        |
|----------------------------|--------------------------------------------------------------------|
| `school-daily-attendance`  | Primary data source — `school_daily_attendance` table              |
| `school-academic-calendar` | `getWorkingDayCount()`, `getWorkingDates()` for date ranges        |
| `school-attendance-rules`  | `getMinAttendancePercentage()` for shortage threshold              |
| `school-section`           | `getStudentsBySection()` for section-scoped reports                |
| `school-student`           | Student name, admission number, roll number for report headers     |

---

## 10. Performance Considerations

- Section summary for 60 students × 90 days = 5,400 records. Queries must use the `(section_id, attendance_date)` index.
- Shortage report queries all active students for the academic year and computes percentages in-application. For large schools (>500 students), use pagination.
- Daily register is bounded by date range; never return more than 90 days at once in V1.
- All report endpoints accept pagination where applicable.

---

## 11. Flyway Migrations

None. This module has no new tables.

---

## 12. Testing Strategy

| Test Type      | Scope                                                                          |
|----------------|--------------------------------------------------------------------------------|
| Unit — Service | `calculateAttendancePercentage`: all status combinations, ON_LEAVE exclusion   |
| Unit — Service | `findConsecutiveAbsences`: streak detection across weekends                    |
| Unit — Service | Shortage: correctly identifies students below threshold                        |
| Controller test| All report endpoints, query param validation, authorization                    |
| Integration    | End-to-end: populate attendance records → run shortage report → verify output  |

---

## 13. Acceptance Criteria

- [ ] Attendance % formula: `(present + late) / workingDays * 100`
- [ ] `ON_LEAVE` days are excluded from both numerator and denominator
- [ ] Shortage report correctly identifies students below configured threshold
- [ ] Daily register covers only working days (no weekends, no holidays)
- [ ] Consecutive absence streak accounts for working days only (not calendar days)
- [ ] All report endpoints require `SCHOOL_ATTENDANCE_REPORT_READ` permission

---

## 14. Out of Scope

- PDF/Excel export
- Scheduled report delivery via email
- Pre-aggregated summary tables (future performance optimization)
- Cross-school reporting
