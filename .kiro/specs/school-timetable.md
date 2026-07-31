# Specification: school-timetable

## 1. Overview

`school-timetable` manages the weekly period schedule for each section within an academic year. A timetable defines which subject is taught by which teacher in which time slot on which day of the week for a given section.

In V1, the timetable is a weekly repeating schedule. There is no support for date-specific overrides within the timetable itself — those are handled by the academic calendar (holidays) and manual attendance corrections.

The timetable is used by `school-daily-attendance` to determine which subjects have periods on a given day, enabling subject-level attendance marking.

---

## 2. Scope and Objectives

**In scope:**
- Defining time slots (periods) for a school
- Creating a weekly timetable for a section within an academic year
- Assigning teacher-subject pairs to time slots on specific days
- Viewing a section's timetable
- Viewing a teacher's timetable across sections
- Timetable status management

**Out of scope:**
- Date-specific timetable overrides (handled by academic calendar + corrections)
- Exam timetable
- Room/venue assignment (V1: no room tracking)
- Conflict detection across teachers/rooms (V1: validated by uniqueness constraints only)

---

## 3. Functional Requirements

### FR-TT-01: Define Time Slots
Create named time slots (periods) for a school: e.g., Period 1 (09:00–09:45), Period 2 (09:45–10:30), Lunch (10:30–11:00). Time slots are school-wide and reused across sections.

### FR-TT-02: Create Timetable Entry
Assign a teacher-assignment (subject-teacher pair in a section) to a time slot on a specific day of the week.

### FR-TT-03: Get Timetable for Section
Return the full weekly timetable for a section: all days × all time slots with their assigned subject-teacher pairs.

### FR-TT-04: Get Timetable for Teacher
Return a teacher's full weekly schedule across all their sections.

### FR-TT-05: Update Timetable Entry
Change the teacher-assignment for an existing timetable slot.

### FR-TT-06: Delete Timetable Entry
Remove a timetable slot assignment. The slot becomes free (no subject/teacher assigned).

### FR-TT-07: Get Subjects with Periods on a Day
Given a section and a day of the week, return the list of subjects that have at least one period. Used by `school-daily-attendance` to know which subjects require attendance marking.

---

## 4. Business Rules

- BR-TT-01: A time slot for a section on a day of the week can have at most one teacher-assignment.
- BR-TT-02: A teacher cannot be assigned to two different section time slots that overlap (same day, same time slot). (V1: enforced by uniqueness constraint on teacher + day + time slot.)
- BR-TT-03: The teacher-assignment must be `ACTIVE` to be used in a timetable entry.
- BR-TT-04: Time slots are ordered by `slotOrder` for display purposes.
- BR-TT-05: The timetable is scoped to an academic year and applies for the full year (no weekly variations in V1).

---

## 5. Domain Model

### SchoolTimeSlot Entity

| Field       | Type          | Description                                    |
|-------------|---------------|------------------------------------------------|
| id          | Long          | Surrogate PK                                   |
| schoolId    | Long          | FK → school_schools(id), NOT NULL              |
| name        | String        | e.g. "Period 1", "Lunch", NOT NULL, max 50     |
| startTime   | LocalTime     | Start time of the slot, NOT NULL               |
| endTime     | LocalTime     | End time of the slot, NOT NULL                 |
| slotOrder   | int           | Display order, NOT NULL                        |
| slotType    | TimeSlotType  | Enum: PERIOD, BREAK, LUNCH                     |
| isDeleted   | boolean       | Soft delete flag                               |
| deletedAt   | LocalDateTime | Soft delete timestamp                          |
| createdAt   | LocalDateTime | Audit                                          |
| updatedAt   | LocalDateTime | Audit                                          |
| createdBy   | Long          | Audit                                          |
| updatedBy   | Long          | Audit                                          |

### TimetableEntry Entity

| Field            | Type          | Description                                         |
|------------------|---------------|-----------------------------------------------------|
| id               | Long          | Surrogate PK                                        |
| schoolId         | Long          | FK → school_schools(id), NOT NULL                   |
| academicYearId   | Long          | FK → school_academic_years(id), NOT NULL            |
| sectionId        | Long          | FK → school_sections(id), NOT NULL                  |
| timeSlotId       | Long          | FK → school_time_slots(id), NOT NULL                |
| dayOfWeek        | DayOfWeek     | Java DayOfWeek enum: MONDAY–SUNDAY, NOT NULL        |
| assignmentId     | Long          | FK → school_teacher_assignments(id), NOT NULL       |
| isDeleted        | boolean       | Soft delete flag                                    |
| deletedAt        | LocalDateTime | Soft delete timestamp                               |
| createdAt        | LocalDateTime | Audit                                               |
| updatedAt        | LocalDateTime | Audit                                               |
| createdBy        | Long          | Audit                                               |
| updatedBy        | Long          | Audit                                               |

### TimeSlotType Enum
- `PERIOD` — teaching period
- `BREAK` — short break
- `LUNCH` — lunch break

---

## 6. Data Model

### Table: `school_time_slots`

```sql
CREATE TABLE school_time_slots (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(50)      NOT NULL,
    start_time  TIME             NOT NULL,
    end_time    TIME             NOT NULL,
    slot_order  INT              NOT NULL,
    slot_type   VARCHAR(20)      NOT NULL DEFAULT 'PERIOD',
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_time_slots_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_time_slots_name (school_id, name),
    INDEX idx_time_slots_school (school_id, slot_order)
);
```

### Table: `school_timetable_entries`

```sql
CREATE TABLE school_timetable_entries (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    time_slot_id     BIGINT UNSIGNED  NOT NULL,
    day_of_week      VARCHAR(10)      NOT NULL,
    assignment_id    BIGINT UNSIGNED  NOT NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_tt_school     FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_tt_year       FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_tt_section    FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_tt_slot       FOREIGN KEY (time_slot_id)     REFERENCES school_time_slots(id),
    CONSTRAINT fk_tt_assignment FOREIGN KEY (assignment_id)    REFERENCES school_teacher_assignments(id),
    UNIQUE uq_timetable_slot (section_id, time_slot_id, day_of_week, academic_year_id),
    INDEX idx_timetable_section (section_id, academic_year_id),
    INDEX idx_timetable_assignment (assignment_id)
);
```

---

## 7. Package Organization

```
com.attendai.school.timetable
├── entity
│   ├── SchoolTimeSlot.java
│   ├── TimetableEntry.java
│   └── TimeSlotType.java
├── repository
│   ├── SchoolTimeSlotRepository.java
│   └── TimetableEntryRepository.java
├── service
│   ├── TimetableService.java
│   └── TimetableServiceImpl.java
├── controller
│   └── TimetableController.java
├── dto
│   ├── CreateTimeSlotRequest.java
│   ├── CreateTimetableEntryRequest.java
│   ├── UpdateTimetableEntryRequest.java
│   ├── TimeSlotResponse.java
│   ├── TimetableEntryResponse.java
│   └── SectionTimetableResponse.java
├── mapper
│   └── TimetableMapper.java
└── exception
    └── TimetableEntryNotFoundException.java
```

---

## 8. API Contracts

### Time Slots

Base path: `/api/v1/school/schools/{schoolId}/time-slots`

#### POST — Create Time Slot

**Permission:** `SCHOOL_TIMETABLE_MANAGE`

**Request:**
```json
{
  "name": "Period 1",
  "startTime": "09:00",
  "endTime": "09:45",
  "slotOrder": 1,
  "slotType": "PERIOD"
}
```

**Response 201:** `TimeSlotResponse`

---

#### GET — List Time Slots

**Permission:** `SCHOOL_TIMETABLE_READ`

**Response 200:** List of `TimeSlotResponse` ordered by `slotOrder`

---

### Timetable Entries

Base path: `/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable`

#### POST /entries — Create Entry

**Permission:** `SCHOOL_TIMETABLE_MANAGE`

**Request:**
```json
{
  "sectionId": 10,
  "timeSlotId": 1,
  "dayOfWeek": "MONDAY",
  "assignmentId": 5
}
```

**Response 201:** `TimetableEntryResponse`

---

#### GET /sections/{sectionId} — Get Section Timetable

**Permission:** `SCHOOL_TIMETABLE_READ`

**Response 200:** `SectionTimetableResponse`
```json
{
  "sectionId": 10,
  "academicYearId": 2,
  "schedule": {
    "MONDAY": [
      { "timeSlot": "Period 1 (09:00–09:45)", "subject": "Mathematics", "teacher": "John Smith" },
      { "timeSlot": "Period 2 (09:45–10:30)", "subject": "English", "teacher": "Jane Doe" }
    ],
    "TUESDAY": [ ... ]
  }
}
```

---

#### GET /teachers/{teacherId} — Get Teacher Timetable

**Permission:** `SCHOOL_TIMETABLE_READ`

**Response 200:** Teacher's weekly schedule across all sections

---

#### PUT /entries/{id}

**Permission:** `SCHOOL_TIMETABLE_MANAGE`

**Response 200:** `TimetableEntryResponse`

---

#### DELETE /entries/{id}

**Permission:** `SCHOOL_TIMETABLE_MANAGE`

**Response 204**

---

## 9. Internal Service API

```
TimetableService.getSubjectsForSectionOnDay(Long sectionId, Long academicYearId, DayOfWeek day): List<SubjectPeriodInfo>
TimetableService.hasTimetableEntry(Long sectionId, Long academicYearId): boolean
```

`getSubjectsForSectionOnDay` is called by `school-daily-attendance` to determine which subjects need attendance marking for a given day.

---

## 10. Authorization

| Operation           | Permission                  |
|---------------------|-----------------------------|
| Manage time slots   | `SCHOOL_TIMETABLE_MANAGE`   |
| Manage entries      | `SCHOOL_TIMETABLE_MANAGE`   |
| Read timetable      | `SCHOOL_TIMETABLE_READ`     |

---

## 11. Flyway Migrations

```
V115__create_school_time_slots_table.sql
V116__create_school_timetable_entries_table.sql
```

---

## 12. Acceptance Criteria

- [ ] A section-timeslot-day combination has at most one entry per academic year
- [ ] A teacher cannot be in two sections at the same time slot on the same day
- [ ] `getSubjectsForSectionOnDay` returns correct subjects for daily attendance
- [ ] Section timetable response is structured by day of week × time slot
- [ ] All timetable modifications produce audit log entries

---

## 13. Out of Scope

- Date-specific timetable overrides
- Exam timetable
- Room/venue assignment
- Automatic conflict detection and resolution
- Substitute teacher scheduling
