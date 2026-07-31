# Specification: school-attendance-rules

## 1. Overview

`school-attendance-rules` defines and manages the configurable attendance rules that govern how attendance is classified and evaluated within a school. These rules feed the daily attendance processing engine and the attendance report module.

Rules are scoped to a school and academic year, with the option to override at the section level.

---

## 2. Scope and Objectives

**In scope:**
- Late arrival threshold time (after which a student is marked LATE instead of PRESENT)
- Minimum attendance percentage requirement (below which a student receives a shortage warning)
- Consecutive absence alert threshold (N consecutive absences triggers a notification)
- Rules scoped to academic year, with optional section-level overrides

**Out of scope:**
- Applying the rules (belongs in `school-daily-attendance`)
- Report generation (belongs in `school-attendance-reports`)
- Leave policy rules (belongs in `school-leave`)

---

## 3. Functional Requirements

### FR-RULES-01: Create Attendance Rule Set
Create a rule set for a school and academic year defining late threshold, minimum percentage, and consecutive absence alert.

### FR-RULES-02: Get Rule Set
Retrieve the rule set for a school-academic year combination, with an optional section-level override.

### FR-RULES-03: Update Rule Set
Modify an existing rule set.

### FR-RULES-04: Create Section Override
Create a rule override specific to a section, overriding one or more school-level rules for that section.

### FR-RULES-05: Delete Section Override
Remove the section-level override; the section falls back to school-level rules.

### FR-RULES-06: Get Effective Rules for Section
Return the effective rules for a section: section override values merged with school-level defaults.

---

## 4. Business Rules

- BR-RULES-01: A school-academic year combination can have exactly one rule set.
- BR-RULES-02: A section can have at most one override per academic year.
- BR-RULES-03: If no rule set exists for a school-academic year, default values from `core-config` are used.
- BR-RULES-04: Late threshold time: default 09:00 AM (configurable). Students arriving after this time are LATE.
- BR-RULES-05: Minimum attendance percentage: default 75% (configurable). Students below this threshold appear in shortage reports.
- BR-RULES-06: Consecutive absence alert: default 3 (configurable). Three consecutive absences triggers a guardian notification.

---

## 5. Domain Model

### AttendanceRuleSet Entity

| Field                     | Type          | Description                                          |
|---------------------------|---------------|------------------------------------------------------|
| id                        | Long          | Surrogate PK                                         |
| schoolId                  | Long          | FK → school_schools(id), NOT NULL                    |
| academicYearId            | Long          | FK → school_academic_years(id), NOT NULL             |
| lateThresholdTime         | LocalTime     | Time after which arrival is LATE, NOT NULL           |
| minAttendancePercentage   | BigDecimal    | Minimum required %, e.g. 75.00, NOT NULL             |
| consecutiveAbsenceAlert   | int           | N consecutive absences → notification                |
| createdAt                 | LocalDateTime | Audit                                                |
| updatedAt                 | LocalDateTime | Audit                                                |
| createdBy                 | Long          | Audit                                                |
| updatedBy                 | Long          | Audit                                                |

### SectionAttendanceRuleOverride Entity

| Field                     | Type          | Description                                          |
|---------------------------|---------------|------------------------------------------------------|
| id                        | Long          | Surrogate PK                                         |
| ruleSetId                 | Long          | FK → school_attendance_rule_sets(id), NOT NULL       |
| sectionId                 | Long          | FK → school_sections(id), NOT NULL                   |
| lateThresholdTime         | LocalTime     | Override, nullable (uses school-level if null)       |
| minAttendancePercentage   | BigDecimal    | Override, nullable                                   |
| consecutiveAbsenceAlert   | int           | Override, 0 = use school-level                       |
| createdAt                 | LocalDateTime | Audit                                                |
| updatedAt                 | LocalDateTime | Audit                                                |
| createdBy                 | Long          | Audit                                                |
| updatedBy                 | Long          | Audit                                                |

---

## 6. Effective Rule Resolution

```
getEffectiveRules(sectionId, academicYearId):
  1. Load school-level AttendanceRuleSet for (schoolId, academicYearId)
  2. Load SectionAttendanceRuleOverride for (sectionId, academicYearId) if exists
  3. Merge: section-level non-null values override school-level values
  4. If no school-level rule set: use core-config defaults
```

---

## 7. Data Model

### Table: `school_attendance_rule_sets`

```sql
CREATE TABLE school_attendance_rule_sets (
    id                        BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id                 BIGINT UNSIGNED  NOT NULL,
    academic_year_id          BIGINT UNSIGNED  NOT NULL,
    late_threshold_time       TIME             NOT NULL DEFAULT '09:00:00',
    min_attendance_percentage DECIMAL(5,2)     NOT NULL DEFAULT 75.00,
    consecutive_absence_alert INT              NOT NULL DEFAULT 3,
    created_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                BIGINT UNSIGNED  NULL,
    updated_by                BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_rule_sets_school FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_rule_sets_year   FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_rule_sets (school_id, academic_year_id)
);
```

### Table: `school_section_attendance_overrides`

```sql
CREATE TABLE school_section_attendance_overrides (
    id                        BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    rule_set_id               BIGINT UNSIGNED  NOT NULL,
    section_id                BIGINT UNSIGNED  NOT NULL,
    late_threshold_time       TIME             NULL,
    min_attendance_percentage DECIMAL(5,2)     NULL,
    consecutive_absence_alert INT              NULL,
    created_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                BIGINT UNSIGNED  NULL,
    updated_by                BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_overrides_rule_set FOREIGN KEY (rule_set_id) REFERENCES school_attendance_rule_sets(id),
    CONSTRAINT fk_overrides_section  FOREIGN KEY (section_id)  REFERENCES school_sections(id),
    UNIQUE uq_section_overrides (rule_set_id, section_id)
);
```

---

## 8. Package Organization

```
com.attendai.school.attendancerules
├── entity
│   ├── AttendanceRuleSet.java
│   └── SectionAttendanceRuleOverride.java
├── repository
│   ├── AttendanceRuleSetRepository.java
│   └── SectionAttendanceRuleOverrideRepository.java
├── service
│   ├── AttendanceRulesService.java
│   └── AttendanceRulesServiceImpl.java
├── controller
│   └── AttendanceRulesController.java
├── dto
│   ├── CreateRuleSetRequest.java
│   ├── UpdateRuleSetRequest.java
│   ├── CreateSectionOverrideRequest.java
│   ├── AttendanceRuleSetResponse.java
│   └── EffectiveRulesResponse.java
├── mapper
│   └── AttendanceRulesMapper.java
└── exception
    └── AttendanceRuleSetNotFoundException.java
```

---

## 9. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/attendance-rules`

### POST — Create Rule Set

**Permission:** `SCHOOL_ATTENDANCE_RULES_MANAGE`

**Request:**
```json
{
  "lateThresholdTime": "09:15",
  "minAttendancePercentage": 75.00,
  "consecutiveAbsenceAlert": 3
}
```

**Response 201:** `AttendanceRuleSetResponse`

---

### GET — Get Rule Set

**Permission:** `SCHOOL_ATTENDANCE_RULES_READ`

**Response 200:** `AttendanceRuleSetResponse`

---

### PUT — Update Rule Set

**Permission:** `SCHOOL_ATTENDANCE_RULES_MANAGE`

**Response 200:** `AttendanceRuleSetResponse`

---

### GET /sections/{sectionId}/effective — Get Effective Rules for Section

**Permission:** `SCHOOL_ATTENDANCE_RULES_READ`

**Response 200:** `EffectiveRulesResponse`

---

### POST /sections/{sectionId}/override — Create Section Override

**Permission:** `SCHOOL_ATTENDANCE_RULES_MANAGE`

**Request:**
```json
{
  "lateThresholdTime": "08:45",
  "minAttendancePercentage": null,
  "consecutiveAbsenceAlert": 2
}
```

**Response 201:** Section override response

---

### DELETE /sections/{sectionId}/override

**Permission:** `SCHOOL_ATTENDANCE_RULES_MANAGE`

**Response 204**

---

## 10. Internal Service API

```
AttendanceRulesService.getEffectiveRules(Long sectionId, Long academicYearId): EffectiveRulesResponse
AttendanceRulesService.getLateThreshold(Long sectionId, Long academicYearId): LocalTime
AttendanceRulesService.getMinAttendancePercentage(Long schoolId, Long academicYearId): BigDecimal
AttendanceRulesService.getConsecutiveAbsenceAlert(Long sectionId, Long academicYearId): int
```

Used by `school-daily-attendance` and `school-attendance-reports`.

---

## 11. Authorization

| Operation        | Permission                           |
|------------------|--------------------------------------|
| Create/update    | `SCHOOL_ATTENDANCE_RULES_MANAGE`     |
| Read             | `SCHOOL_ATTENDANCE_RULES_READ`       |
| Section override | `SCHOOL_ATTENDANCE_RULES_MANAGE`     |

---

## 12. Flyway Migrations

```
V118__create_school_attendance_rule_sets_table.sql
V119__create_school_section_attendance_overrides_table.sql
```

---

## 13. Acceptance Criteria

- [ ] Only one rule set per school-academic year combination
- [ ] Section override correctly overrides only the specified fields
- [ ] `getEffectiveRules` falls back to `core-config` when no rule set exists
- [ ] Late threshold change takes effect on the next processing cycle
- [ ] All write operations produce audit log entries

---

## 14. Out of Scope

- Applying rules (school-daily-attendance)
- Leave policy rules (school-leave)
- Per-subject attendance rules (V1: day-level only)
