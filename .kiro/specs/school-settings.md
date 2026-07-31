# Specification: school-settings

## 1. Overview

`school-settings` manages school-level configuration overrides that control the behavior of the AttendAI School module for a specific school. These settings extend and override the platform-level defaults stored in `core-config`.

School settings are key-value pairs scoped to a school. They are the primary way an administrator customizes school-specific operational behavior without affecting other schools on the platform.

---

## 2. Scope and Objectives

**In scope:**
- School-level key-value configuration store
- Override of Core and School default configuration values
- School-specific settings for:
  - Weekend configuration (which days are non-working)
  - Attendance processing schedule
  - Mark-absent time
  - Student login enablement
  - Minimum attendance percentage (overrides attendance rules default)
  - Leave balance enforcement mode
  - Notification preferences (per school)
  - Four-eyes principle for attendance corrections
- Startup seeding of default school settings

**Out of scope:**
- Attendance rule sets (managed by `school-attendance-rules` with their own entity)
- Per-section overrides (managed by section-level rule overrides)
- System-wide platform settings (managed by `core-config`)

---

## 3. Functional Requirements

### FR-SET-01: Get Setting
Retrieve the value of a specific setting key for a school. Falls back to the school module default if not set.

### FR-SET-02: Set Setting
Create or update a setting key-value pair for a school.

### FR-SET-03: Delete Setting
Delete a school-specific setting. The system falls back to the module default after deletion.

### FR-SET-04: List All Settings for School
Return all settings configured for a school, showing both the current school value and the default.

### FR-SET-05: Get Effective Setting
Return the effective value for a key for a school: school-level setting → school module default → `core-config` default.

### FR-SET-06: Startup Seeding
On first access for a school, seed default setting values.

---

## 4. School Setting Keys

| Key                                       | Default   | Type    | Description                                  |
|-------------------------------------------|-----------|---------|----------------------------------------------|
| `school.weekend.days`                     | `SAT,SUN` | String  | Comma-separated weekend day codes            |
| `school.attendance.processing.enabled`    | `true`    | Boolean | Enable/disable event processing for school   |
| `school.attendance.mark-absent.time`      | `11:00`   | String  | Time to run the mark-absent job (HH:mm)      |
| `school.attendance.notify.absent`         | `true`    | Boolean | Notify guardian when student is marked absent|
| `school.attendance.notify.late`           | `false`   | Boolean | Notify guardian when student is marked late  |
| `school.student.login.enabled`            | `false`   | Boolean | Allow students to have login accounts        |
| `school.leave.balance.enforce`            | `false`   | Boolean | Hard-reject leave when balance insufficient  |
| `school.corrections.four-eyes`            | `true`    | Boolean | Require different user to approve correction |
| `school.dashboard.cache.ttl-minutes`      | `2`       | Integer | Dashboard cache TTL in minutes               |
| `school.attendance.consecutive-alert`     | `3`       | Integer | Consecutive absent days before alert fires   |

---

## 5. Domain Model

### SchoolSetting Entity

| Field       | Type          | Description                                             |
|-------------|---------------|---------------------------------------------------------|
| id          | Long          | Surrogate PK                                            |
| schoolId    | Long          | FK → school_schools(id), NOT NULL                       |
| settingKey  | String        | Setting key, max 200, NOT NULL                          |
| settingValue| String        | Setting value, max 1000, NOT NULL                       |
| description | String        | Optional, max 500                                       |
| createdAt   | LocalDateTime | Audit                                                   |
| updatedAt   | LocalDateTime | Audit                                                   |
| createdBy   | Long          | Audit                                                   |
| updatedBy   | Long          | Audit                                                   |

---

## 6. Data Model

### Table: `school_settings`

```sql
CREATE TABLE school_settings (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id     BIGINT UNSIGNED  NOT NULL,
    setting_key   VARCHAR(200)     NOT NULL,
    setting_value VARCHAR(1000)    NOT NULL,
    description   VARCHAR(500)     NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    BIGINT UNSIGNED  NULL,
    updated_by    BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_settings_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_settings (school_id, setting_key),
    INDEX idx_school_settings_school (school_id)
);
```

---

## 7. Effective Setting Resolution

```
getEffectiveSetting(schoolId, key):
  1. Look up SchoolSetting for (schoolId, key)
  2. If found → return school-specific value
  3. If not found → look up school-module default (in-code constant map)
  4. If not found → delegate to ConfigService.getString(key, defaultValue) from core-config
```

---

## 8. Caching Strategy

School settings are small and frequently read (e.g., weekend days checked on every attendance event processing cycle). Implement an in-memory cache per school with TTL-based invalidation:

- Cache key: `(schoolId, settingKey)`
- TTL: 5 minutes (configurable)
- Evict on `set()` or `delete()` for the affected key

---

## 9. Package Organization

```
com.attendai.school.settings
├── entity
│   └── SchoolSetting.java
├── repository
│   └── SchoolSettingRepository.java
├── service
│   ├── SchoolSettingsService.java
│   └── SchoolSettingsServiceImpl.java
├── cache
│   └── SchoolSettingsCache.java
├── controller
│   └── SchoolSettingsController.java
├── dto
│   ├── SetSchoolSettingRequest.java
│   ├── SchoolSettingResponse.java
│   └── SchoolSettingsSummaryResponse.java
└── mapper
    └── SchoolSettingMapper.java
```

---

## 10. API Contracts

Base path: `/api/v1/school/schools/{schoolId}/settings`

### GET — List All Settings

**Permission:** `SCHOOL_SETTINGS_READ`

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "key": "school.weekend.days",
      "value": "SAT,SUN",
      "defaultValue": "SAT,SUN",
      "description": "Comma-separated weekend day codes"
    },
    {
      "key": "school.attendance.mark-absent.time",
      "value": "10:30",
      "defaultValue": "11:00",
      "description": "Custom mark-absent time for this school"
    }
  ]
}
```

---

### GET /{key} — Get Setting

**Permission:** `SCHOOL_SETTINGS_READ`

**Response 200:** `SchoolSettingResponse`
**Response 404:** Key not found (returns default value in body, not 404 — never throws for missing settings)

---

### PUT /{key} — Set Setting

**Permission:** `SCHOOL_SETTINGS_MANAGE`

**Request:**
```json
{
  "value": "10:30",
  "description": "School starts earlier, mark absent at 10:30"
}
```

**Response 200:** `SchoolSettingResponse`

---

### DELETE /{key} — Reset to Default

**Permission:** `SCHOOL_SETTINGS_MANAGE`

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "Setting reset to default", "defaultValue": "11:00" }
}
```

---

## 11. Validation Rules

| Field        | Rule                                               |
|--------------|----------------------------------------------------|
| key          | Must be a recognized school setting key            |
| value        | Not blank, max 1000, type-appropriate for the key  |

Type validation per key:
- Boolean keys: `true` or `false`
- Time keys: `HH:mm` format
- Integer keys: valid integer within allowed range
- `school.weekend.days`: comma-separated list of valid `DayOfWeek` abbreviations

---

## 12. Authorization

| Operation          | Permission                  |
|--------------------|-----------------------------|
| Read settings      | `SCHOOL_SETTINGS_READ`      |
| Write settings     | `SCHOOL_SETTINGS_MANAGE`    |

---

## 13. Internal Service API

```
SchoolSettingsService.getString(Long schoolId, String key): String
SchoolSettingsService.getString(Long schoolId, String key, String defaultValue): String
SchoolSettingsService.getBoolean(Long schoolId, String key): boolean
SchoolSettingsService.getBoolean(Long schoolId, String key, boolean defaultValue): boolean
SchoolSettingsService.getLocalTime(Long schoolId, String key): LocalTime
SchoolSettingsService.getWeekendDays(Long schoolId): Set<DayOfWeek>
SchoolSettingsService.isStudentLoginEnabled(Long schoolId): boolean
SchoolSettingsService.isFourEyesEnabled(Long schoolId): boolean
```

Used by:
- `school-academic-calendar` — `getWeekendDays(schoolId)` for `isWorkingDay()` calculation
- `school-daily-attendance` — `getMarkAbsentTime()`, `isNotifyAbsentEnabled()`
- `school-leave` — `isLeaveBalanceEnforced()`
- `school-attendance-corrections` — `isFourEyesEnabled()`
- `school-dashboard` — `getDashboardCacheTtl()`

---

## 14. Integration Points

| Module                     | Integration                                              |
|----------------------------|----------------------------------------------------------|
| `core-config`              | Fallback for keys not set at school level                |
| `school-school`            | `schoolId` FK; all settings scoped to a school          |
| `school-academic-calendar` | Reads weekend days configuration                         |
| `school-daily-attendance`  | Reads mark-absent time and notification settings         |
| `school-leave`             | Reads leave balance enforcement flag                     |
| `school-attendance-corrections` | Reads four-eyes enforcement flag                  |
| `core-audit`               | Audit events for all setting changes                     |

---

## 15. Error Handling

| Scenario                  | Handling                                        |
|---------------------------|-------------------------------------------------|
| Unknown setting key        | `ValidationException` (400) — unknown key      |
| Invalid value format       | `ValidationException` (400) — type mismatch    |
| Missing setting (read)     | Returns default value — never throws           |

---

## 16. Logging and Audit

| Action           | Audit Code              | Details                         |
|------------------|-------------------------|---------------------------------|
| Setting changed  | `SCHOOL_SETTING_CHANGED`| school_id, key, old_value, new_value |
| Setting deleted  | `SCHOOL_SETTING_DELETED`| school_id, key                  |

---

## 17. Flyway Migrations

```
V122__create_school_settings_table.sql
```

---

## 18. Testing Strategy

| Test Type       | Scope                                                              |
|-----------------|--------------------------------------------------------------------|
| Unit — Service  | `getString`: school value, default value, core-config fallback     |
| Unit — Service  | `getWeekendDays`: parse comma-separated day codes                  |
| Unit — Cache    | Cache hit bypasses DB; set/delete evicts cache                     |
| Repository test | `findBySchoolIdAndSettingKey`, school scoping                      |
| Controller test | List, get, set, reset endpoints; key validation                    |
| Integration     | Change mark-absent time → verify processing job picks up new value |

---

## 19. Acceptance Criteria

- [ ] Reading an unset key returns the default without throwing
- [ ] Setting `school.weekend.days` to include `FRI` makes Friday a non-working day
- [ ] `isStudentLoginEnabled()` returns correct value from school setting
- [ ] Setting changes are immediately reflected in `SchoolSettingsCache`
- [ ] All setting writes produce audit log entries with old and new values
- [ ] Unknown key write returns 400

---

## 20. Out of Scope

- Platform-wide configuration (core-config)
- Per-section configuration (school-attendance-rules section overrides)
- UI settings / theme preferences
- Integration with external HRMS or SIS configuration
