# Specification: core-station

## 1. Overview

`core-station` manages the attendance stations that capture attendance events on the AttendAI platform. An attendance station is any device or endpoint that records a person's presence — a physical kiosk with a camera, a tablet at an entry point, a QR code scanner, or an API endpoint consumed by a third-party device.

`core-station` is domain-agnostic. A station does not know whether it serves a school, a college, or an enterprise. Business modules associate stations with their domain context (e.g., a school associates a station with its campus).

`core-station` is responsible for station registration, status management, API key authentication for stations, configuration, and heartbeat/health tracking.

---

## 2. Scope and Objectives

**In scope:**
- Station registration and CRUD
- Station status management (ACTIVE, INACTIVE, MAINTENANCE)
- Station type classification (ENTRY, EXIT, ENTRY_EXIT, MANUAL)
- Station API key generation and management (for station-to-server authentication)
- Station heartbeat recording (last-seen timestamp)
- Station configuration key-value overrides (per-station settings)
- Location information for stations

**Out of scope:**
- Station hardware firmware management
- Video streaming from stations
- Station physical provisioning
- Business module associations (e.g., linking a station to a specific school classroom — belongs in the business module)

---

## 3. Functional Requirements

### FR-STATION-01: Register Station
Create a new station record with a name, type, location details, and optional description. On creation, generate an API key for the station.

### FR-STATION-02: Get Station by ID
Retrieve a single non-deleted station by its surrogate ID.

### FR-STATION-03: Get Station by API Key
Retrieve a station by its API key. Used by the authentication filter to identify which station is making a request.

### FR-STATION-04: List Stations
Return a paginated list of stations. Filterable by status and type.

### FR-STATION-05: Update Station
Update station name, description, location, and type.

### FR-STATION-06: Change Station Status
Transition a station between ACTIVE, INACTIVE, and MAINTENANCE states.

### FR-STATION-07: Delete Station (Soft)
Soft-delete a station. A station cannot be deleted if it has associated attendance events.

### FR-STATION-08: Regenerate API Key
Generate a new API key for a station and invalidate the old one. Used when a key is compromised.

### FR-STATION-09: Record Heartbeat
Update the station's `lastSeenAt` timestamp. Called by stations periodically to report they are online.

### FR-STATION-10: Get Station Configuration
Retrieve the effective configuration for a station: a merged view of system-level defaults from `core-config` overridden by station-specific key-value pairs.

### FR-STATION-11: Set Station Configuration Override
Store a station-specific configuration key-value override that takes precedence over system-level defaults for that station.

---

## 4. Non-Functional Requirements

- Station API key lookup must use an indexed hashed column for fast authentication.
- Station API keys must be cryptographically random (minimum 32 bytes, Base64-encoded).
- API keys are stored hashed (SHA-256) in the database. The raw key is shown only once on generation.
- The heartbeat endpoint must be extremely lightweight — no business logic, just a timestamp update.
- A station is considered offline if `lastSeenAt` is more than 5 minutes ago (configurable threshold).

---

## 5. Business Rules

- BR-STATION-01: Station names must be unique within the platform.
- BR-STATION-02: API keys are shown in plain text only at the moment of creation or regeneration. They are stored hashed.
- BR-STATION-03: A deleted station's API key is invalidated immediately.
- BR-STATION-04: A station in `MAINTENANCE` status can receive heartbeats but cannot submit attendance events.
- BR-STATION-05: A station in `INACTIVE` status cannot receive heartbeats or submit attendance events.
- BR-STATION-06: A station cannot be deleted if it has recorded attendance events.

---

## 6. Station Type Classification

| Type         | Description                                              |
|--------------|----------------------------------------------------------|
| `ENTRY`      | Records entry (check-in) events only                     |
| `EXIT`       | Records exit (check-out) events only                     |
| `ENTRY_EXIT` | Records both entry and exit; device determines direction |
| `MANUAL`     | Admin manually records events via this station           |

---

## 7. Station Status State Machine

```
[Registered] → ACTIVE
ACTIVE       → INACTIVE     (admin deactivates)
ACTIVE       → MAINTENANCE  (admin sets maintenance)
INACTIVE     → ACTIVE       (admin activates)
MAINTENANCE  → ACTIVE       (admin clears maintenance)
Any          → [Deleted]    (soft delete, if no events)
```

---

## 8. Domain Model

### Station Entity

| Field        | Type           | Description                                           |
|--------------|----------------|-------------------------------------------------------|
| id           | Long           | Surrogate PK                                          |
| name         | String         | Unique, NOT NULL, max 255                             |
| type         | StationType    | Enum: ENTRY, EXIT, ENTRY_EXIT, MANUAL                 |
| status       | StationStatus  | Enum: ACTIVE, INACTIVE, MAINTENANCE                   |
| description  | String         | Optional, max 1000                                    |
| locationName | String         | Human-readable location, max 255                      |
| latitude     | BigDecimal     | Optional, GPS latitude                                |
| longitude    | BigDecimal     | Optional, GPS longitude                               |
| apiKeyHash   | String         | SHA-256 hash of the API key, NOT NULL, UNIQUE         |
| lastSeenAt   | LocalDateTime  | Timestamp of last heartbeat, nullable                 |
| isDeleted    | boolean        | Soft delete flag                                      |
| deletedAt    | LocalDateTime  | Soft delete timestamp                                 |
| createdAt    | LocalDateTime  | Audit                                                 |
| updatedAt    | LocalDateTime  | Audit                                                 |
| createdBy    | Long           | Audit                                                 |
| updatedBy    | Long           | Audit                                                 |

### StationConfig Entity

| Field      | Type          | Description                              |
|------------|---------------|------------------------------------------|
| id         | Long          | Surrogate PK                             |
| stationId  | Long          | FK → stations(id), NOT NULL              |
| configKey  | String        | Configuration key, max 100              |
| configValue| String        | Configuration value, max 1000           |
| createdAt  | LocalDateTime | Audit                                    |
| updatedAt  | LocalDateTime | Audit                                    |
| createdBy  | Long          | Audit                                    |
| updatedBy  | Long          | Audit                                    |

---

## 9. Data Model

### Table: `stations`

```sql
CREATE TABLE stations (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255)     NOT NULL,
    type          VARCHAR(20)      NOT NULL,
    status        VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    description   TEXT             NULL,
    location_name VARCHAR(255)     NULL,
    latitude      DECIMAL(10,7)    NULL,
    longitude     DECIMAL(10,7)    NULL,
    api_key_hash  VARCHAR(64)      NOT NULL,
    last_seen_at  DATETIME         NULL,
    is_deleted    BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME         NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    BIGINT UNSIGNED  NULL,
    updated_by    BIGINT UNSIGNED  NULL,

    UNIQUE uq_stations_name (name),
    UNIQUE uq_stations_api_key_hash (api_key_hash),
    INDEX idx_stations_status (status),
    INDEX idx_stations_type (type)
);
```

### Table: `station_configs`

```sql
CREATE TABLE station_configs (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    station_id   BIGINT UNSIGNED  NOT NULL,
    config_key   VARCHAR(100)     NOT NULL,
    config_value VARCHAR(1000)    NOT NULL,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_station_configs_station FOREIGN KEY (station_id) REFERENCES stations(id),
    UNIQUE uq_station_configs (station_id, config_key),
    INDEX idx_station_configs_station_id (station_id)
);
```

---

## 10. Station Authentication

Stations authenticate to the API using their API key passed in a request header:

```
X-Station-Api-Key: <raw-api-key>
```

The `StationAuthenticationFilter` (in `core-auth` or `core-station`, registered in `SecurityConfig`):
1. Extracts the `X-Station-Api-Key` header.
2. Hashes the raw key with SHA-256.
3. Looks up the station by the hash.
4. Validates station is ACTIVE.
5. Sets a `StationAuthentication` in the Spring Security context with a synthetic authority of `ROLE_STATION`.

Stations are granted `CORE_FACE_RECOGNIZE` and `CORE_ATTENDANCE_RECORD` authorities, allowing them to submit face recognition queries and record attendance events. They do not hold human user authorities.

---

## 11. Package Organization

```
com.attendai.core.station
├── entity
│   ├── Station.java
│   ├── StationConfig.java
│   ├── StationType.java
│   └── StationStatus.java
├── repository
│   ├── StationRepository.java
│   └── StationConfigRepository.java
├── service
│   ├── StationService.java
│   └── StationServiceImpl.java
├── controller
│   └── StationController.java
├── dto
│   ├── CreateStationRequest.java
│   ├── UpdateStationRequest.java
│   ├── ChangeStationStatusRequest.java
│   ├── StationConfigRequest.java
│   ├── StationResponse.java
│   ├── StationSummaryResponse.java
│   └── StationRegistrationResponse.java
├── mapper
│   └── StationMapper.java
└── exception
    ├── StationNotFoundException.java
    └── StationAlreadyExistsException.java
```

---

## 12. API Contracts

Base path: `/api/v1/core/stations`

### POST /api/v1/core/stations — Register Station

**Permission:** `CORE_STATION_CREATE`

**Request:**
```json
{
  "name": "Main Entrance",
  "type": "ENTRY_EXIT",
  "description": "Primary entrance station",
  "locationName": "Building A, Ground Floor",
  "latitude": 19.076090,
  "longitude": 72.877426
}
```

**Response 201:** `StationRegistrationResponse` (includes the raw API key — only time it is shown)
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Main Entrance",
    "type": "ENTRY_EXIT",
    "status": "ACTIVE",
    "apiKey": "ak_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
    "createdAt": "2025-01-01T00:00:00Z"
  }
}
```

---

### GET /api/v1/core/stations/{id}

**Permission:** `CORE_STATION_READ`

**Response 200:** `StationResponse` (no `apiKey` field)

---

### GET /api/v1/core/stations

**Permission:** `CORE_STATION_READ`

**Query params:** `page`, `size`, `status`, `type`

**Response 200:** Paginated `StationSummaryResponse`

---

### PUT /api/v1/core/stations/{id}

**Permission:** `CORE_STATION_UPDATE`

**Response 200:** `StationResponse`

---

### PATCH /api/v1/core/stations/{id}/status

**Permission:** `CORE_STATION_UPDATE`

**Request:**
```json
{ "status": "MAINTENANCE", "reason": "Camera replacement" }
```

**Response 200:** `StationResponse`

---

### POST /api/v1/core/stations/{id}/regenerate-key

**Permission:** `CORE_STATION_UPDATE`

**Response 200:** `StationRegistrationResponse` (new raw API key shown once)

---

### DELETE /api/v1/core/stations/{id}

**Permission:** `CORE_STATION_DELETE`

**Response 204**
**Response 409:** Station has attendance events

---

### POST /api/v1/core/stations/heartbeat

**Authentication:** `X-Station-Api-Key` header (station auth, no user JWT required)

**Request:** Empty body or `{}`

**Response 200:**
```json
{
  "success": true,
  "data": { "timestamp": "2025-01-15T10:30:00Z" }
}
```

---

### GET /api/v1/core/stations/{id}/config

**Permission:** `CORE_STATION_READ`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "stationId": 1,
    "config": {
      "face.recognition.threshold": "0.90",
      "face.liveness.enabled": "true"
    }
  }
}
```

---

### PUT /api/v1/core/stations/{id}/config

**Permission:** `CORE_STATION_UPDATE`

**Request:**
```json
{
  "key": "face.recognition.threshold",
  "value": "0.90"
}
```

**Response 200:** Updated config map

---

## 13. Validation Rules

### CreateStationRequest
| Field       | Rule                                              |
|-------------|---------------------------------------------------|
| name        | Not blank, max 255, unique                        |
| type        | Not null, valid `StationType` enum                |
| description | Optional, max 1000                                |
| locationName| Optional, max 255                                 |
| latitude    | Optional, range -90.0 to 90.0                     |
| longitude   | Optional, range -180.0 to 180.0                   |

---

## 14. Authorization

| Operation                | Required Permission    |
|--------------------------|------------------------|
| Register station         | `CORE_STATION_CREATE`  |
| Read station             | `CORE_STATION_READ`    |
| List stations            | `CORE_STATION_READ`    |
| Update station           | `CORE_STATION_UPDATE`  |
| Change status            | `CORE_STATION_UPDATE`  |
| Regenerate API key       | `CORE_STATION_UPDATE`  |
| Delete station           | `CORE_STATION_DELETE`  |
| Heartbeat                | Station API key only   |
| Get station config       | `CORE_STATION_READ`    |
| Set station config       | `CORE_STATION_UPDATE`  |

---

## 15. Internal Service API

Exposed as Spring beans:

```
StationService.findByApiKeyHash(String hash): Optional<Station>
StationService.existsById(Long id): boolean
StationService.isActiveById(Long id): boolean
StationService.getEffectiveConfig(Long stationId): Map<String, String>
```

Used by:
- `core-auth` / `SecurityConfig` — station API key authentication filter
- `core-attendance` — validates station ID when recording events

---

## 16. Integration Points

| Module           | Integration                                                         |
|------------------|---------------------------------------------------------------------|
| `core-common`    | `SoftDeletableEntity`, exceptions, response types                   |
| `core-config`    | System-level config defaults merged with station-specific overrides |
| `core-attendance`| `station_id` FK on `attendance_events`; station validity check      |
| `core-audit`     | Audit events for station CRUD, status changes, key regeneration     |

---

## 17. Error Handling

| Scenario                               | Exception                       | HTTP |
|----------------------------------------|---------------------------------|------|
| Station not found                      | `StationNotFoundException`      | 404  |
| Station name already exists            | `StationAlreadyExistsException` | 409  |
| Invalid API key                        | `UnauthorizedException`         | 401  |
| Inactive/deleted station API key used  | `UnauthorizedException`         | 401  |
| Delete station with events             | `ValidationException`           | 409  |
| Invalid status transition              | `ValidationException`           | 400  |

---

## 18. Logging and Audit

| Action                   | Audit Code                   | Details                   |
|--------------------------|------------------------------|---------------------------|
| Station registered       | `STATION_CREATED`            | station_id, name          |
| Station updated          | `STATION_UPDATED`            | station_id                |
| Station status changed   | `STATION_STATUS_CHANGED`     | station_id, old, new      |
| Station deleted          | `STATION_DELETED`            | station_id                |
| API key regenerated      | `STATION_KEY_REGENERATED`    | station_id                |
| Heartbeat received       | INFO log only (no audit log) | station_id, timestamp     |

---

## 19. Flyway Migrations

```
V13__create_stations_table.sql
V14__create_station_configs_table.sql
```

---

## 20. Testing Strategy

| Test Type       | Scope                                                             |
|-----------------|-------------------------------------------------------------------|
| Unit — Service  | Register, update, status transitions, delete guard                |
| Unit — Service  | API key generation, hashing, regeneration                         |
| Unit — Service  | Effective config merge (station overrides system defaults)        |
| Repository test | `findByApiKeyHash`, status filter, soft-delete exclusion          |
| Controller test | All endpoints, HTTP codes, registration response includes API key |
| Security tests  | Admin endpoints require JWT; heartbeat requires station API key   |
| Integration     | Register station → heartbeat → verify lastSeenAt updated          |

---

## 21. Implementation Roadmap

### Task 1: Entity and migrations
- `Station`, `StationConfig` entities
- Flyway: `V13`, `V14`

### Task 2: API key utilities
- Secure random key generation (`SecureRandom`, Base64)
- SHA-256 hashing utility

### Task 3: Service — CRUD
- `createStation` (generate key, hash, store), `findById`, `listStations`, `updateStation`
- `changeStatus` with transition validation
- `deleteStation` with event guard
- `regenerateApiKey`

### Task 4: Station authentication
- `StationAuthenticationFilter` using `X-Station-Api-Key` header
- Register in `SecurityConfig`

### Task 5: Heartbeat
- `recordHeartbeat(Long stationId)` — lightweight `UPDATE stations SET last_seen_at = NOW()`

### Task 6: Configuration
- `getEffectiveConfig` — merge `core-config` system defaults with `station_configs` overrides
- `setConfigOverride`

### Task 7: Controller and DTOs
- `StationController`, `StationMapper`

### Task 8: Audit integration

---

## 22. Acceptance Criteria

- [ ] Raw API key is shown only once (at creation or regeneration)
- [ ] API key is stored as SHA-256 hash; never returned in subsequent responses
- [ ] Station API key authentication grants `ROLE_STATION` authority
- [ ] An INACTIVE station's API key is rejected with 401
- [ ] Deleting a station with attendance events returns 409
- [ ] Heartbeat endpoint responds in under 20ms
- [ ] Station effective config correctly merges system defaults with station-specific overrides
- [ ] All CRUD operations produce audit log entries

---

## 23. Out of Scope

- Station firmware OTA updates
- Streaming video from stations
- Business-domain station associations (which school/classroom a station belongs to)
- Station grouping or zone management
- Physical device provisioning workflows
