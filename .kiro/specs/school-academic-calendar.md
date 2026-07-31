# Specification: school-academic-calendar

## 1. Overview

`school-academic-calendar` is the authority for determining which dates within an academic year are working days, holidays, or declared working days (a holiday converted back into a working day).

**The academic calendar is a mandatory gateway for all attendance processing.** Before recording or computing any student attendance for a given date, the system must consult the academic calendar to confirm that the date is a working day. Attendance must not be processed on a non-working day.

Administrators manage the calendar by declaring holidays, editing them, deleting them, and converting holidays back into working days.

---

## 2. Scope and Objectives

**In scope:**
- Holiday creation within an academic year
- Holiday editing and deletion
- Working day declarations (explicit marking of days as working, overriding weekends or system defaults)
- Converting a holiday back into a working day
- Calendar view: day-by-day working/holiday status for a month or academic year
- Working day check API used by attendance processing
- Weekend configuration per school (stored in `school-settings`)

**Out of scope:**
- National public holiday seeding (administrator manages all entries manually)
- Timetable-level period scheduling (belongs in `school-timetable`)
- Exam schedules or event calendars beyond attendance relevance

---

## 3. Functional Requirements

### FR-CAL-01: Create Holiday
An administrator creates a holiday entry for a specific date within an academic year. A holiday can span a single day or a date range. Each day in a range is stored as an individual `CalendarEntry` record.

### FR-CAL-02: Edit Holiday
Update the holiday name or description for an existing holiday entry.

### FR-CAL-03: Delete Holiday
Delete a holiday entry. After deletion, the date reverts to its default state (working day or weekend based on school settings).

### FR-CAL-04: Declare Working Day
Explicitly mark a specific date as a working day. Used to convert a weekend or holiday into a working school day (e.g., a Saturday makeup class).

### FR-CAL-05: Convert Holiday to Working Day
Change an existing holiday entry's type to `WORKING_DAY`, effectively cancelling the holiday.

### FR-CAL-06: Get Calendar for Month
Return the calendar state for every day in a given month within an academic year: working day, holiday, or weekend.

### FR-CAL-07: Get Calendar for Academic Year
Return all calendar entries for an entire academic year as a list.

### FR-CAL-08: Check if Date is Working Day
Given a school ID, academic year ID, and a date, return whether that date is a working day. This is the core query used by attendance processing.

### FR-CAL-09: Get Working Day Count
Given a date range within an academic year, return the total number of working days. Used by attendance reports and percentage calculations.

---

## 4. Non-Functional Requirements

- `isWorkingDay(schoolId, academicYearId, date)` is called on every attendance event processing cycle. It must respond in under 5ms. This is achieved by caching the full calendar for an academic year in memory, invalidated on any calendar change.
- The calendar for a single academic year should have at most ~250 entries (one per school working day). Queries are always date-range bounded.
- All calendar operations must be scoped to a specific school and academic year.

---

## 5. Business Rules

- BR-CAL-01: A date can have at most one calendar entry within an academic year and school. Duplicate date entries are rejected.
- BR-CAL-02: A calendar entry date must fall within the academic year's start and end date range.
- BR-CAL-03: If no explicit entry exists for a date, the system derives the status from school settings: weekdays are working days, weekends (Saturday, Sunday by default) are non-working. School settings can configure different weekend days.
- BR-CAL-04: Attendance must not be processed for a date that is not a working day.
- BR-CAL-05: A holiday entry for a date prevents attendance processing for that date, regardless of day of week.
- BR-CAL-06: A `WORKING_DAY` entry for a weekend date makes that date eligible for attendance.
- BR-CAL-07: Calendar entries can only be created/modified for the `ACTIVE` academic year, or for `UPCOMING` years (pre-planning). `COMPLETED` year calendars are read-only.

---

## 6. Calendar Entry Types

| Type          | Description                                              |
|---------------|----------------------------------------------------------|
| `HOLIDAY`     | Date is a non-working day (school declared holiday)      |
| `WORKING_DAY` | Date is explicitly marked as a working day (overrides weekend/holiday default) |

Working days that follow the default schedule (weekday = working) do NOT have a calendar entry. Only exceptions are stored.

---

## 7. Domain Model

### SchoolCalendarEntry Entity

| Field          | Type            | Description                                            |
|----------------|-----------------|--------------------------------------------------------|
| id             | Long            | Surrogate PK                                           |
| schoolId       | Long            | FK → school_schools(id), NOT NULL                      |
| academicYearId | Long            | FK → school_academic_years(id), NOT NULL               |
| entryDate      | LocalDate       | The specific date, NOT NULL                            |
| entryType      | CalendarEntryType| Enum: HOLIDAY, WORKING_DAY                            |
| name           | String          | e.g. "Diwali", "Republic Day", max 200                 |
| description    | String          | Optional, max 500                                      |
| createdAt      | LocalDateTime   | Audit                                                  |
| updatedAt      | LocalDateTime   | Audit                                                  |
| createdBy      | Long            | Audit                                                  |
| updatedBy      | Long            | Audit                                                  |

Note: No `is_deleted` — calendar entries are hard-deleted. Deleting a holiday entry is a definitive operation that reverts the date to default status. Soft-delete adds confusion since the absence of an entry has semantic meaning.

### CalendarEntryType Enum
- `HOLIDAY`
- `WORKING_DAY`

---

## 8. Working Day Determination Algorithm

```
isWorkingDay(schoolId, academicYearId, date):

  1. Look up CalendarEntry for (schoolId, academicYearId, date)
  2. If entry found:
     - HOLIDAY → return false (not a working day)
     - WORKING_DAY → return true (explicitly a working day)
  3. If no entry found:
     - Determine day of week for the date
     - Look up school's weekend days from SchoolSettings
       (default: SATURDAY, SUNDAY)
     - If date's day-of-week is in weekend days → return false
     - Otherwise → return true (regular weekday)
```

---

## 9. Data Model

### Table: `school_calendar_entries`

```sql
CREATE TABLE school_calendar_entries (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    entry_date       DATE             NOT NULL,
    entry_type       VARCHAR(20)      NOT NULL,
    name             VARCHAR(200)     NOT NULL,
    description      VARCHAR(500)     NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_cal_entries_school  FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_cal_entries_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_cal_entries_date (school_id, academic_year_id, entry_date),
    INDEX idx_cal_entries_year (school_id, academic_year_id),
    INDEX idx_cal_entries_date_range (school_id, academic_year_id, entry_date)
);
```

---

## 10. Package Organization

```
com.attendai.school.academiccalendar
├── entity
│   ├── SchoolCalendarEntry.java
│   └── CalendarEntryType.java
├── repository
│   └── SchoolCalendarEntryRepository.java
├── service
│   ├── AcademicCalendarService.java
│   └── AcademicCalendarServiceImpl.java
├── controller
│   └── AcademicCalendarController.java
├── dto
│   ├── CreateHolidayRequest.java
│   ├── CreateHolidayRangeRequest.java
│   ├── UpdateCalendarEntryRequest.java
│   ├── DeclareWorkingDayRequest.java
│   ├── CalendarEntryResponse.java
│   └── MonthCalendarResponse.java
├── mapper
│   └── AcademicCalendarMapper.java
└── exception
    └── CalendarEntryNotFoundException.java
```

---

## 11. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/calendar`

### POST /holidays — Create Holiday

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Request (single day):**
```json
{
  "date": "2025-10-24",
  "name": "Dussehra",
  "description": "National holiday"
}
```

**Response 201:** `CalendarEntryResponse`

---

### POST /holidays/range — Create Holiday Range

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Request:**
```json
{
  "startDate": "2025-10-24",
  "endDate": "2025-10-26",
  "name": "Dussehra Break",
  "description": "Three-day holiday"
}
```

**Response 201:** List of `CalendarEntryResponse` (one per day)

---

### PUT /entries/{id} — Update Calendar Entry

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Request:** `{ "name": "Dussehra (Revised)", "description": "Updated" }`

**Response 200:** `CalendarEntryResponse`

---

### DELETE /entries/{id} — Delete Calendar Entry

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Response 204** — date reverts to default status

---

### POST /working-days — Declare Working Day

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Request:**
```json
{
  "date": "2025-11-08",
  "name": "Makeup Saturday",
  "description": "Compensatory working day for holiday on 24th Oct"
}
```

**Response 201:** `CalendarEntryResponse`

---

### PATCH /entries/{id}/convert-to-working-day — Convert Holiday to Working Day

**Permission:** `SCHOOL_CALENDAR_MANAGE`

**Response 200:** `CalendarEntryResponse` with `entryType = WORKING_DAY`

---

### GET /month — Get Calendar for Month

**Permission:** `SCHOOL_CALENDAR_READ`

**Query params:** `year` (calendar year, e.g. 2025), `month` (1–12)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "year": 2025,
    "month": 10,
    "days": [
      { "date": "2025-10-01", "dayOfWeek": "WEDNESDAY", "status": "WORKING_DAY", "entryName": null },
      { "date": "2025-10-04", "dayOfWeek": "SATURDAY", "status": "WEEKEND", "entryName": null },
      { "date": "2025-10-24", "dayOfWeek": "FRIDAY", "status": "HOLIDAY", "entryName": "Dussehra" }
    ]
  }
}
```

---

### GET /entries — Get All Calendar Entries for Academic Year

**Permission:** `SCHOOL_CALENDAR_READ`

**Query params:** `entryType` (optional), `fromDate`, `toDate`

**Response 200:** List of `CalendarEntryResponse`

---

### GET /working-days/count — Get Working Day Count

**Permission:** `SCHOOL_CALENDAR_READ`

**Query params:** `fromDate`, `toDate`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "fromDate": "2025-06-01",
    "toDate": "2025-10-31",
    "workingDayCount": 98
  }
}
```

---

## 12. Validation Rules

### CreateHolidayRequest
| Field       | Rule                                                             |
|-------------|------------------------------------------------------------------|
| date        | Not null, must fall within academic year start–end range         |
| name        | Not blank, max 200                                               |
| description | Optional, max 500                                                |

### CreateHolidayRangeRequest
| Field      | Rule                                                              |
|------------|-------------------------------------------------------------------|
| startDate  | Not null, within academic year range                             |
| endDate    | Not null, >= startDate, within academic year range               |
| name       | Not blank, max 200                                               |

---

## 13. Authorization

| Operation                   | Permission                  |
|-----------------------------|-----------------------------|
| Create/edit/delete entries  | `SCHOOL_CALENDAR_MANAGE`    |
| Declare working day         | `SCHOOL_CALENDAR_MANAGE`    |
| Convert holiday to working  | `SCHOOL_CALENDAR_MANAGE`    |
| Read calendar               | `SCHOOL_CALENDAR_READ`      |

---

## 14. Internal Service API

```
AcademicCalendarService.isWorkingDay(Long schoolId, Long academicYearId, LocalDate date): boolean
AcademicCalendarService.getWorkingDayCount(Long schoolId, Long academicYearId, LocalDate from, LocalDate to): int
AcademicCalendarService.getWorkingDates(Long schoolId, Long academicYearId, LocalDate from, LocalDate to): List<LocalDate>
```

`isWorkingDay` implements the Working Day Determination Algorithm from section 8. It is the central guard for all attendance processing.

---

## 15. Caching Strategy

The full set of calendar entries for an academic year is small (max ~250 entries per year per school). The `isWorkingDay` check is called on every attendance event. Therefore:

- Cache the list of `CalendarEntry` records per `(schoolId, academicYearId)` in an in-memory `ConcurrentHashMap` in `AcademicCalendarServiceImpl`.
- Invalidate the cache whenever a calendar entry is created, updated, or deleted for that `(schoolId, academicYearId)`.
- Weekend configuration is read from `SchoolSettingsService` (cached separately).

This eliminates a DB query per attendance event.

---

## 16. Integration Points

| Module                    | Integration                                                       |
|---------------------------|-------------------------------------------------------------------|
| `school-academic-year`    | Calendar entries are scoped to an academic year                   |
| `school-school`           | `schoolId` scoping; weekend config from school settings           |
| `school-settings`         | Weekend days configuration (SATURDAY/SUNDAY by default)           |
| `school-daily-attendance` | `isWorkingDay()` called before processing every attendance event  |
| `school-attendance-reports`| `getWorkingDayCount()` used in percentage calculations           |
| `core-audit`              | Audit events for all calendar entry changes                       |

---

## 17. Error Handling

| Scenario                              | Exception                        | HTTP |
|---------------------------------------|----------------------------------|------|
| Calendar entry not found              | `CalendarEntryNotFoundException` | 404  |
| Duplicate entry for date              | `ResourceAlreadyExistsException` | 409  |
| Date outside academic year range      | `ValidationException`            | 400  |
| Modifying COMPLETED year calendar     | `ValidationException`            | 400  |

---

## 18. Logging and Audit

| Action                    | Audit Code               | Details                         |
|---------------------------|--------------------------|---------------------------------|
| Holiday created           | `CALENDAR_HOLIDAY_CREATED` | entry_id, date, name          |
| Holiday updated           | `CALENDAR_ENTRY_UPDATED`   | entry_id                      |
| Holiday deleted           | `CALENDAR_ENTRY_DELETED`   | entry_id, date                |
| Working day declared      | `CALENDAR_WORKING_DAY_DECLARED` | entry_id, date             |
| Holiday converted         | `CALENDAR_HOLIDAY_CONVERTED`    | entry_id, date             |

---

## 19. Flyway Migrations

```
V108__create_school_calendar_entries_table.sql
```

---

## 20. Testing Strategy

| Test Type       | Scope                                                                       |
|-----------------|-----------------------------------------------------------------------------|
| Unit — Service  | `isWorkingDay`: weekday (no entry), weekend, holiday, WORKING_DAY override  |
| Unit — Service  | `getWorkingDayCount`: date ranges, mixed holidays and weekends               |
| Unit — Service  | Duplicate date rejection, date outside year range rejection                 |
| Unit — Cache    | Cache invalidation on create/update/delete                                  |
| Repository test | `findBySchoolAndYearAndDate`, range queries                                 |
| Controller test | All endpoints, month calendar response structure                             |
| Integration     | Create holiday → attempt attendance on that date → verify rejection         |

---

## 21. Acceptance Criteria

- [ ] `isWorkingDay` returns false for a declared holiday
- [ ] `isWorkingDay` returns false for a weekend (per school settings)
- [ ] `isWorkingDay` returns true for a declared `WORKING_DAY` on a weekend
- [ ] Attendance processing is blocked for non-working days
- [ ] Duplicate date entries are rejected with 409
- [ ] Calendar entries outside the academic year date range are rejected
- [ ] `getWorkingDayCount` accurately counts working days in a range including holiday and working-day overrides
- [ ] Cache is invalidated immediately when a calendar entry is modified
- [ ] COMPLETED year calendar is read-only

---

## 22. Out of Scope

- National holiday auto-seeding
- Exam schedule management
- Event or activity calendar
- Term/semester sub-divisions
