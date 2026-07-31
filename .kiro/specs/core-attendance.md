# Specification: core-attendance

## 1. Overview

`core-attendance` is the attendance event processing engine for the AttendAI platform. It receives raw attendance events from stations (face recognition results, QR scans, manual entry), validates and processes them, and produces a canonical attendance record.

`core-attendance` is deliberately domain-agnostic. It does not know about school sessions, shift hours, or lecture periods. It records attendance events as timestamped facts against a person. Business modules consume these events and apply domain-specific rules (e.g., marking a student present for a school day, calculating employee shift hours).

This separation is fundamental to the platform's extensibility contract.

---

## 2. Scope and Objectives

**In scope:**
- Receiving and persisting attendance events from stations
- Deduplication of rapid repeated events (same person, same station, same direction within a configurable window)
- Attendance event status management (PENDING, PROCESSED, REJECTED, DUPLICATE)
- Event source tracking (FACE_RECOGNITION, QR_CODE, MANUAL, API)
- Event direction tracking (ENTRY, EXIT, UNSPECIFIED)
- Querying events by person, station, date range
- Providing an event stream API for business modules to consume
- Supporting manual attendance recording (admin creates an event directly)

**Out of scope:**
- Attendance rules processing (e.g., late/absent thresholds — belongs in business modules)
- Daily attendance summarization (belongs in business modules)
- Leave management (belongs in business modules)
- Reports (belongs in business modules)
- Academic calendar awareness (belongs in school module)

---

## 3. Functional Requirements

### FR-ATT-01: Record Attendance Event from Station
A station submits an attendance event containing: person identification (personId or face recognition result), event time, station ID, event direction, and source. The system validates the inputs, applies deduplication, and persists the event.

### FR-ATT-02: Record Manual Attendance Event
An authorized user creates an attendance event manually: provide personId, eventTime, stationId (optional for manual), direction, and a reason note.

### FR-ATT-03: Event Deduplication
If the same person triggers multiple events at the same station with the same direction within a configurable time window (default: 5 minutes), additional events are stored with status `DUPLICATE` and do not generate downstream notifications or processing triggers.

### FR-ATT-04: Event Status Management
Events transition through a status lifecycle:
- `PENDING` — just recorded, awaiting downstream processing
- `PROCESSED` — consumed and processed by a business module
- `REJECTED` — rejected (e.g., person not found, station inactive)
- `DUPLICATE` — flagged as a duplicate within the deduplication window

### FR-ATT-05: Mark Event as Processed
Business modules call this internal API to mark events as `PROCESSED` after consuming them. This prevents double-processing.

### FR-ATT-06: Get Attendance Events for Person
Return a paginated list of attendance events for a given person, filterable by date range, status, and direction.

### FR-ATT-07: Get Attendance Events for Station
Return a paginated list of attendance events recorded at a given station, filterable by date range.

### FR-ATT-08: Get Attendance Events (General Query)
Return attendance events filterable by: personId, stationId, status, source, direction, date range. Supports pagination.

### FR-ATT-09: Get Pending Events for Processing
Business modules poll for `PENDING` events they need to process. Returns events with `status = PENDING` ordered by `eventTime` ascending. Supports pagination.

### FR-ATT-10: Correct Attendance Event
An authorized user corrects an existing event: change the person, event time, direction, or reason. Corrections create a new event with `source = CORRECTION` and link to the original event. The original event is not modified.

---

## 4. Non-Functional Requirements

- Station event submission must respond within 200ms.
- Deduplication check must use an indexed query on `(person_id, station_id, direction, event_time)`.
- The events table will grow large over time. Partitioning by month is recommended for tables exceeding 1M rows (future consideration).
- `PENDING` event polling by business modules must support high-frequency polling without lock contention.
- Manual event recording must be audit-logged.

---

## 5. Business Rules

- BR-ATT-01: An attendance event must have a valid `personId` referencing a non-deleted person.
- BR-ATT-02: An attendance event must have a valid `stationId` if the source is not `MANUAL` or `API`.
- BR-ATT-03: The station must be in `ACTIVE` status to accept events. `MAINTENANCE` and `INACTIVE` stations are rejected.
- BR-ATT-04: `eventTime` must not be more than 1 minute in the future (clock skew tolerance).
- BR-ATT-05: `eventTime` must not be more than 24 hours in the past (anti-backdating).
- BR-ATT-06: Deduplication window is configurable per station (default: 300 seconds / 5 minutes).
- BR-ATT-07: A `DUPLICATE` event is persisted but does not trigger downstream business module processing.
- BR-ATT-08: Manual events bypass deduplication logic.
- BR-ATT-09: An event correction creates a new event; the original event gets a `correctedById` back-reference.

---

## 6. Attendance Event Status State Machine

```
[Received]  → PENDING
PENDING     → PROCESSED  (business module calls markAsProcessed)
PENDING     → DUPLICATE  (deduplication check triggers on receipt)
PENDING     → REJECTED   (validation failure: inactive station, unknown person)
PROCESSED   → [immutable]
REJECTED    → [immutable]
DUPLICATE   → [immutable]

[Correction] → Creates new PENDING event linked to original
```

---

## 7. Domain Model

### AttendanceEvent Entity

| Field           | Type                  | Description                                                    |
|-----------------|-----------------------|----------------------------------------------------------------|
| id              | Long                  | Surrogate PK                                                   |
| personId        | Long                  | FK → persons(id), NOT NULL                                     |
| stationId       | Long                  | FK → stations(id), nullable for MANUAL source                  |
| eventTime       | LocalDateTime         | When the attendance was recorded (UTC), NOT NULL               |
| direction       | EventDirection        | Enum: ENTRY, EXIT, UNSPECIFIED                                 |
| source          | EventSource           | Enum: FACE_RECOGNITION, QR_CODE, MANUAL, API, CORRECTION       |
| status          | AttendanceEventStatus | Enum: PENDING, PROCESSED, REJECTED, DUPLICATE                  |
| rejectionReason | String                | Reason for rejection, max 500, nullable                        |
| notes           | String                | Optional operator notes, max 500                               |
| originalEventId | Long                  | FK → attendance_events(id), set for CORRECTION source          |
| processedAt     | LocalDateTime         | When marked as PROCESSED, nullable                             |
| processedBy     | String                | Module that processed this event (e.g., "school"), nullable    |
| createdAt       | LocalDateTime         | Audit                                                          |
| updatedAt       | LocalDateTime         | Audit                                                          |
| createdBy       | Long                  | Audit (userId for manual; null for station)                    |
| updatedBy       | Long                  | Audit                                                          |

### EventDirection Enum
- `ENTRY` — person entering
- `EXIT` — person exiting
- `UNSPECIFIED` — direction not determined (e.g., ENTRY_EXIT station without direction detection)

### EventSource Enum
- `FACE_RECOGNITION` — identified via face recognition
- `QR_CODE` — identified via QR code scan
- `MANUAL` — manually recorded by an operator
- `API` — submitted via API by an external system
- `CORRECTION` — correction of a previous event

### AttendanceEventStatus Enum
- `PENDING` — recorded, awaiting processing
- `PROCESSED` — consumed by a business module
- `REJECTED` — rejected due to validation failure
- `DUPLICATE` — flagged as duplicate within deduplication window

---

## 8. Entity Relationships

```
persons (core-person)
    │
    │ 1:N
    ▼
attendance_events (core-attendance)
    │
    │ N:1
    ▼
stations (core-station)
```

Self-reference for corrections:
```
attendance_events
    │ originalEventId FK (self-reference)
    └── attendance_events (original)
```

---

## 9. Data Model

### Table: `attendance_events`

```sql
CREATE TABLE attendance_events (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    person_id        BIGINT UNSIGNED  NOT NULL,
    station_id       BIGINT UNSIGNED  NULL,
    event_time       DATETIME         NOT NULL,
    direction        VARCHAR(20)      NOT NULL DEFAULT 'UNSPECIFIED',
    source           VARCHAR(30)      NOT NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    rejection_reason VARCHAR(500)     NULL,
    notes            VARCHAR(500)     NULL,
    original_event_id BIGINT UNSIGNED NULL,
    processed_at     DATETIME         NULL,
    processed_by     VARCHAR(50)      NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_attendance_events_person  FOREIGN KEY (person_id)         REFERENCES persons(id),
    CONSTRAINT fk_attendance_events_station FOREIGN KEY (station_id)        REFERENCES stations(id),
    CONSTRAINT fk_attendance_events_original FOREIGN KEY (original_event_id) REFERENCES attendance_events(id),

    INDEX idx_attendance_events_person_id (person_id),
    INDEX idx_attendance_events_station_id (station_id),
    INDEX idx_attendance_events_event_time (event_time),
    INDEX idx_attendance_events_status (status),
    INDEX idx_attendance_events_person_time (person_id, event_time),
    INDEX idx_attendance_events_dedup (person_id, station_id, direction, event_time)
);
```

---

## 10. Deduplication Logic

When an event arrives for `(personId, stationId, direction)`:

```
1. Query for the most recent PENDING or PROCESSED event for
   the same (person_id, station_id, direction) within the last N seconds
   (N = station deduplication window, default 300).

2. If such an event exists:
   → Persist the new event with status = DUPLICATE
   → Return 200 (not rejected; the event is stored for audit)

3. If no such event exists:
   → Persist the new event with status = PENDING
   → Trigger downstream notification if configured
```

Deduplication does not apply to MANUAL or CORRECTION events.

---

## 11. Station Event Submission Flow

```
Station → POST /api/v1/core/attendance/events
         (X-Station-Api-Key: <key>, body: { personId, eventTime, direction, source })
    │
    ▼
[AttendanceController]
    ├── Authenticate station (StationAuthenticationFilter already ran)
    ├── @Valid on request DTO
    │
    ▼
[AttendanceServiceImpl.recordStationEvent()]
    ├── Validate personId exists
    ├── Validate stationId is ACTIVE
    ├── Validate eventTime constraints
    ├── Run deduplication check
    │   ├── DUPLICATE → persist with DUPLICATE status → return result
    │   └── NEW → persist with PENDING status
    ├── Write audit log
    └── Return AttendanceEventResponse
```

---

## 12. Package Organization

```
com.attendai.core.attendance
├── entity
│   ├── AttendanceEvent.java
│   ├── EventDirection.java
│   ├── EventSource.java
│   └── AttendanceEventStatus.java
├── repository
│   └── AttendanceEventRepository.java
├── service
│   ├── AttendanceService.java
│   └── AttendanceServiceImpl.java
├── controller
│   └── AttendanceController.java
├── dto
│   ├── RecordAttendanceEventRequest.java
│   ├── RecordManualEventRequest.java
│   ├── CorrectAttendanceEventRequest.java
│   ├── AttendanceEventFilter.java
│   ├── AttendanceEventResponse.java
│   └── AttendanceEventSummaryResponse.java
├── mapper
│   └── AttendanceEventMapper.java
└── exception
    └── AttendanceEventNotFoundException.java
```

---

## 13. API Contracts

Base path: `/api/v1/core/attendance`

### POST /api/v1/core/attendance/events — Record Event from Station

**Authentication:** `X-Station-Api-Key` header (station auth)

**Request:**
```json
{
  "personId": 101,
  "eventTime": "2025-01-15T09:00:00",
  "direction": "ENTRY",
  "source": "FACE_RECOGNITION",
  "notes": null
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "id": 5001,
    "personId": 101,
    "stationId": 3,
    "eventTime": "2025-01-15T09:00:00Z",
    "direction": "ENTRY",
    "source": "FACE_RECOGNITION",
    "status": "PENDING"
  }
}
```

---

### POST /api/v1/core/attendance/events/manual — Record Manual Event

**Permission:** `CORE_ATTENDANCE_RECORD_MANUAL`

**Request:**
```json
{
  "personId": 101,
  "eventTime": "2025-01-15T09:05:00",
  "direction": "ENTRY",
  "notes": "Station was offline, recorded manually"
}
```

**Response 201:** `AttendanceEventResponse`

---

### GET /api/v1/core/attendance/events/{id}

**Permission:** `CORE_ATTENDANCE_READ`

**Response 200:** `AttendanceEventResponse`

---

### GET /api/v1/core/attendance/events

**Permission:** `CORE_ATTENDANCE_READ`

**Query params:** `personId`, `stationId`, `status`, `source`, `direction`, `fromDate`, `toDate`, `page`, `size`

**Response 200:** Paginated `AttendanceEventSummaryResponse`

---

### GET /api/v1/core/attendance/events/pending

**Permission:** `CORE_ATTENDANCE_PROCESS` (held by business module service accounts)

**Query params:** `page`, `size`, `processedBy` (filter by module namespace)

**Response 200:** Paginated `AttendanceEventResponse`

---

### PATCH /api/v1/core/attendance/events/{id}/process

**Permission:** `CORE_ATTENDANCE_PROCESS`

**Request:**
```json
{ "processedBy": "school" }
```

**Response 200:** `AttendanceEventResponse` with `status = PROCESSED`

---

### POST /api/v1/core/attendance/events/{id}/correct

**Permission:** `CORE_ATTENDANCE_CORRECT`

**Request:**
```json
{
  "personId": 102,
  "eventTime": "2025-01-15T09:10:00",
  "direction": "ENTRY",
  "notes": "Corrected wrong person identification"
}
```

**Response 201:** New `AttendanceEventResponse` with `source = CORRECTION`, `originalEventId` set

---

## 14. Request Validation Rules

### RecordAttendanceEventRequest
| Field     | Rule                                                            |
|-----------|-----------------------------------------------------------------|
| personId  | Not null, must reference existing non-deleted person           |
| eventTime | Not null, not more than 1 min future, not more than 24h past   |
| direction | Not null, valid `EventDirection` enum                          |
| source    | Not null, valid `EventSource` enum                             |
| notes     | Optional, max 500                                              |

### RecordManualEventRequest
| Field     | Rule                                                            |
|-----------|-----------------------------------------------------------------|
| personId  | Not null                                                        |
| eventTime | Not null, not future                                            |
| direction | Not null                                                        |
| notes     | Optional, max 500                                              |

---

## 15. Authorization

| Operation                    | Required                             |
|------------------------------|--------------------------------------|
| Record station event         | Station API key (ROLE_STATION)       |
| Record manual event          | `CORE_ATTENDANCE_RECORD_MANUAL`      |
| Read events                  | `CORE_ATTENDANCE_READ`               |
| Get pending events           | `CORE_ATTENDANCE_PROCESS`            |
| Mark as processed            | `CORE_ATTENDANCE_PROCESS`            |
| Correct event                | `CORE_ATTENDANCE_CORRECT`            |

---

## 16. Internal Service API

Exposed as Spring beans to business modules:

```
AttendanceService.findPendingEventsForPerson(Long personId, LocalDate date): List<AttendanceEventResponse>
AttendanceService.findEventsByPersonAndDateRange(Long personId, LocalDateTime from, LocalDateTime to): List<AttendanceEventResponse>
AttendanceService.markAsProcessed(Long eventId, String processedBy): void
AttendanceService.countEventsByPersonAndDate(Long personId, LocalDate date): int
```

These are the primary integration points for business modules to implement their own attendance rules on top of Core events.

---

## 17. Configuration

| Property                                    | Default | Description                              |
|---------------------------------------------|---------|------------------------------------------|
| `attendai.attendance.dedup-window-seconds`  | `300`   | Default deduplication window in seconds  |
| `attendai.attendance.max-future-seconds`    | `60`    | Max seconds an event can be in the future|
| `attendai.attendance.max-past-hours`        | `24`    | Max hours an event can be backdated      |

---

## 18. Integration Points

| Module           | Integration                                                         |
|------------------|---------------------------------------------------------------------|
| `core-common`    | `BaseEntity`, exceptions, response types                            |
| `core-person`    | Validates `personId` on event receipt                               |
| `core-station`   | Validates station status; station ID from authenticated station     |
| `core-face`      | Face recognition result feeds `personId` into attendance event      |
| `core-audit`     | Audit events for all attendance event writes                        |
| `core-notification` | Optional: send notification on PENDING event (configurable)     |
| Business modules | Poll `PENDING` events; call `markAsProcessed`                       |

---

## 19. Error Handling

| Scenario                             | Exception                          | HTTP |
|--------------------------------------|------------------------------------|------|
| Event not found                      | `AttendanceEventNotFoundException` | 404  |
| Person not found                     | `ResourceNotFoundException`        | 404  |
| Station not ACTIVE                   | `ValidationException`              | 400  |
| Event time in future (>1 min)        | `ValidationException`              | 400  |
| Event time too far in past (>24h)    | `ValidationException`              | 400  |
| Marking non-PENDING event processed  | `ValidationException`              | 400  |

---

## 20. Logging and Audit

| Action                   | Audit Code                     | Details                              |
|--------------------------|--------------------------------|--------------------------------------|
| Event recorded (station) | `ATTENDANCE_EVENT_RECORDED`    | event_id, person_id, station_id      |
| Event recorded (manual)  | `ATTENDANCE_EVENT_MANUAL`      | event_id, person_id, user_id         |
| Event marked duplicate   | `ATTENDANCE_EVENT_DUPLICATE`   | event_id, original_event_id          |
| Event rejected           | `ATTENDANCE_EVENT_REJECTED`    | event_id, reason                     |
| Event processed          | `ATTENDANCE_EVENT_PROCESSED`   | event_id, processed_by               |
| Event corrected          | `ATTENDANCE_EVENT_CORRECTED`   | new_event_id, original_event_id      |

---

## 21. Performance and Scalability

- The `attendance_events` table is the highest-volume table in the system.
- Key indexes: `(person_id, event_time)` for person-based queries; `(status)` for pending polling; `(person_id, station_id, direction, event_time)` for deduplication.
- Deduplication query must be a single indexed lookup, not a full table scan.
- For deployments with >1M events, table partitioning by `event_time` (monthly) is the recommended future optimization.
- Pending event polling uses `status = 'PENDING'` with `ORDER BY event_time ASC LIMIT n` — this must be index-covered.

---

## 22. Edge Cases and Failure Scenarios

| Scenario                                              | Handling                                         |
|-------------------------------------------------------|--------------------------------------------------|
| Same person scans twice within dedup window           | Second event stored as DUPLICATE                 |
| Station clock skew (event_time slightly in future)    | 1-minute tolerance window accepted               |
| Person person deleted after event recorded            | Event references person_id; event preserved      |
| Station goes offline mid-batch                        | Each event is committed individually             |
| Business module fails to process an event             | Event remains PENDING; module retries on next poll|
| Manual event without stationId                        | Permitted; stationId is nullable for MANUAL source|
| Correction of a CORRECTION event                      | Permitted; creates a chain via originalEventId   |

---

## 23. Flyway Migrations

```
V15__create_attendance_events_table.sql
```

---

## 24. Testing Strategy

| Test Type       | Scope                                                                     |
|-----------------|---------------------------------------------------------------------------|
| Unit — Service  | Record event: valid, duplicate, rejected scenarios                        |
| Unit — Service  | Deduplication window logic (boundary values)                              |
| Unit — Service  | Manual event, correction event, markAsProcessed                           |
| Repository test | Indexed queries: by person+date, by status, dedup check                   |
| Controller test | Station event endpoint (station auth), manual endpoint (user auth)        |
| Security tests  | Station endpoint requires station key; manual requires user permission    |
| Integration     | Station submits event → business module polls pending → marks processed   |

---

## 25. Implementation Roadmap

### Task 1: Entity and migration
- `AttendanceEvent` entity, enums
- `AttendanceEventRepository`
- Flyway: `V15__create_attendance_events_table.sql`

### Task 2: Core service — record event
- `recordStationEvent()` with validation + deduplication
- `recordManualEvent()` bypassing deduplication

### Task 3: Core service — queries
- `findById`, `findEventsByFilter` (general query with specification or JPQL)
- `findPendingEventsForPerson`, `findEventsByPersonAndDateRange`

### Task 4: Core service — processing
- `markAsProcessed`, `correctEvent`

### Task 5: Controller and DTOs
- `AttendanceController` with all endpoints
- `AttendanceEventMapper`

### Task 6: Configuration
- `AttendanceProperties` `@ConfigurationProperties`

### Task 7: Audit integration
- Write audit events for all event state changes

---

## 26. Acceptance Criteria

- [ ] Identical station events within 5 minutes are stored as DUPLICATE
- [ ] Events from an INACTIVE station are rejected with status `REJECTED`
- [ ] Events more than 1 minute in the future are rejected
- [ ] Manual events bypass deduplication
- [ ] `markAsProcessed` only works on PENDING events
- [ ] Corrected events create a new event with `source = CORRECTION` and `originalEventId` set
- [ ] `findPendingEventsForPerson` returns events in `eventTime` ascending order
- [ ] All attendance state changes are audit-logged

---

## 27. Out of Scope

- Attendance rule processing (late/absent/present — belongs in business modules)
- Daily attendance summary tables (belongs in business modules)
- Shift or schedule management (belongs in enterprise module)
- Academic calendar attendance awareness (belongs in school module)
- Real-time WebSocket push of attendance events
