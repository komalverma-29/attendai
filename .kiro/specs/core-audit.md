# Specification: core-audit

## 1. Overview

`core-audit` provides immutable, append-only audit logging for all significant events in the AttendAI platform. It records who did what, to which resource, at what time, and from which IP address.

Every Core module and every business module writes audit events through the `AuditService` Spring bean. The audit module persists these events to a dedicated table that is never updated or soft-deleted. Audit records are permanent.

`core-audit` also exposes a query API for administrators to search and review the audit log.

---

## 2. Scope and Objectives

**In scope:**
- Accepting and persisting audit events from any module
- Storing: actor (user ID), action code, resource type, resource ID, IP address, timestamp, module, additional details (JSON)
- Querying audit logs with flexible filters: actor, action, resource, date range, module
- Paginated audit log retrieval
- Audit event validation (required fields enforced)

**Out of scope:**
- Real-time audit event streaming or WebSocket push
- Log aggregation tools (ELK, Splunk) — out of scope for V1
- Automatic PII masking in audit details
- Audit log archival or purging (records are permanent in V1)

---

## 3. Functional Requirements

### FR-AUDIT-01: Write Audit Event
Accept an audit event containing: actor user ID, action code, resource type, resource ID, IP address, module name, and optional additional details (JSON string). Persist immediately. This call must be synchronous and reliable.

### FR-AUDIT-02: Query Audit Log
Return a paginated list of audit events filterable by: actorUserId, actionCode, resourceType, resourceId, module, IP address, and date range. Results are ordered by `occurredAt` descending.

### FR-AUDIT-03: Get Audit Event by ID
Retrieve a single audit event by its surrogate ID.

### FR-AUDIT-04: Audit Log is Immutable
No update or delete operations exist on the `audit_logs` table. Neither the service API nor any repository method exposes mutation of existing records.

---

## 4. Non-Functional Requirements

- Audit writes must complete within 20ms under normal load.
- `AuditService.log()` must never throw an exception that propagates to the caller. If the write fails, it is logged at ERROR level and the caller continues normally.
- The `audit_logs` table has no `is_deleted` column and no `updated_at` column.
- Audit log queries must support date range filters efficiently via the `occurred_at` index.
- The audit log is expected to grow to millions of records. Index design must account for this.

---

## 5. Business Rules

- BR-AUDIT-01: Audit records are immutable. They are never updated or deleted.
- BR-AUDIT-02: `actorUserId` is nullable — system-initiated actions (scheduled jobs, station events) may not have a user actor.
- BR-AUDIT-03: `actionCode` must be a non-blank string following the `<MODULE>_<ENTITY>_<ACTION>` naming convention.
- BR-AUDIT-04: `ipAddress` is nullable — internal service calls may not carry an IP.
- BR-AUDIT-05: `details` is a JSON string. It is stored as-is. Consumers are responsible for interpreting it.
- BR-AUDIT-06: The audit table must never be truncated or modified by application code under any circumstances.

---

## 6. Domain Model

### AuditLog Entity

| Field          | Type          | Description                                                    |
|----------------|---------------|----------------------------------------------------------------|
| id             | Long          | Surrogate PK                                                   |
| actorUserId    | Long          | User who performed the action; nullable for system actions     |
| actionCode     | String        | Action code, e.g. `USER_CREATED`, `AUTH_LOGIN_SUCCESS`, max 100|
| resourceType   | String        | Entity type, e.g. `User`, `Person`, `AttendanceEvent`, max 100 |
| resourceId     | String        | String representation of the resource ID, max 100             |
| module         | String        | Module that wrote the event, e.g. `core-auth`, `school`, max 50|
| ipAddress      | String        | IP address of the request, nullable, max 45 (IPv6 max length) |
| details        | String        | JSON string with event-specific context, TEXT, nullable        |
| occurredAt     | LocalDateTime | When the event occurred (UTC), NOT NULL                        |
| createdAt      | LocalDateTime | When the record was inserted                                   |

Note: No `updatedAt`, no `is_deleted`, no `createdBy`, no `updatedBy` — audit records are never modified.

---

## 7. Data Model

### Table: `audit_logs`

```sql
CREATE TABLE audit_logs (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT UNSIGNED  NULL,
    action_code   VARCHAR(100)     NOT NULL,
    resource_type VARCHAR(100)     NULL,
    resource_id   VARCHAR(100)     NULL,
    module        VARCHAR(50)      NOT NULL,
    ip_address    VARCHAR(45)      NULL,
    details       TEXT             NULL,
    occurred_at   DATETIME         NOT NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_logs_actor (actor_user_id),
    INDEX idx_audit_logs_action_code (action_code),
    INDEX idx_audit_logs_resource (resource_type, resource_id),
    INDEX idx_audit_logs_occurred_at (occurred_at),
    INDEX idx_audit_logs_module (module),
    INDEX idx_audit_logs_actor_date (actor_user_id, occurred_at)
);
```

No foreign key on `actor_user_id` — audit records must survive even if the user is deleted. The reference is intentionally unconstrained.

---

## 8. Standard Action Codes

All modules use the following naming convention: `<ENTITY>_<ACTION>`.

### Core Auth
| Code                    | Description                |
|-------------------------|----------------------------|
| `AUTH_LOGIN_SUCCESS`    | Successful login            |
| `AUTH_LOGIN_FAILURE`    | Failed login attempt        |
| `AUTH_LOGOUT`           | User logged out             |
| `AUTH_TOKEN_REFRESH`    | Token refreshed             |
| `AUTH_RESET_REQUEST`    | Password reset requested    |
| `AUTH_RESET_SUCCESS`    | Password reset completed    |
| `AUTH_TOKEN_REUSE`      | Token reuse detected        |

### Core User
| Code                     | Description              |
|--------------------------|--------------------------|
| `USER_CREATED`           | User account created      |
| `USER_UPDATED`           | User account updated      |
| `USER_STATUS_CHANGED`    | Status changed            |
| `USER_DELETED`           | User soft-deleted         |
| `USER_PASSWORD_CHANGED`  | Password changed          |

### Core Person
| Code             | Description     |
|------------------|-----------------|
| `PERSON_CREATED` | Person created  |
| `PERSON_UPDATED` | Person updated  |
| `PERSON_DELETED` | Person deleted  |

### Core Role / Permission
| Code                         | Description                  |
|------------------------------|------------------------------|
| `ROLE_CREATED`               | Role created                 |
| `ROLE_ASSIGNED_TO_USER`      | Role assigned to user        |
| `ROLE_REMOVED_FROM_USER`     | Role removed from user       |
| `PERMISSION_ASSIGNED_TO_ROLE`| Permission assigned to role  |
| `PERMISSION_REMOVED_FROM_ROLE`| Permission removed from role|

### Core Attendance
| Code                          | Description                   |
|-------------------------------|-------------------------------|
| `ATTENDANCE_EVENT_RECORDED`   | Station event recorded        |
| `ATTENDANCE_EVENT_MANUAL`     | Manual event recorded         |
| `ATTENDANCE_EVENT_DUPLICATE`  | Event flagged as duplicate    |
| `ATTENDANCE_EVENT_REJECTED`   | Event rejected                |
| `ATTENDANCE_EVENT_PROCESSED`  | Event marked processed        |
| `ATTENDANCE_EVENT_CORRECTED`  | Event corrected               |

### Core Face
| Code                        | Description           |
|-----------------------------|-----------------------|
| `FACE_PROFILE_CREATED`      | Face profile created  |
| `FACE_IMAGE_ENROLLED`       | Face image enrolled   |
| `FACE_IMAGE_REMOVED`        | Face image removed    |
| `FACE_PROFILE_ACTIVATED`    | Profile activated     |
| `FACE_PROFILE_DEACTIVATED`  | Profile deactivated   |
| `FACE_PROFILE_DELETED`      | Profile deleted       |
| `FACE_RECOGNITION_MATCH`    | Recognition match     |
| `FACE_RECOGNITION_NO_MATCH` | No match              |

### Core File
| Code            | Description    |
|-----------------|----------------|
| `FILE_UPLOADED` | File uploaded  |
| `FILE_DOWNLOADED`| File downloaded|
| `FILE_DELETED`  | File deleted   |

### Core Station
| Code                      | Description            |
|---------------------------|------------------------|
| `STATION_CREATED`         | Station created        |
| `STATION_STATUS_CHANGED`  | Status changed         |
| `STATION_DELETED`         | Station deleted        |
| `STATION_KEY_REGENERATED` | API key regenerated    |

---

## 9. Package Organization

```
com.attendai.core.audit
├── entity
│   └── AuditLog.java
├── repository
│   └── AuditLogRepository.java
├── service
│   ├── AuditService.java
│   └── AuditServiceImpl.java
├── controller
│   └── AuditController.java
├── dto
│   ├── AuditLogResponse.java
│   ├── AuditLogFilter.java
│   └── WriteAuditEventRequest.java     ← internal DTO used by other modules
├── mapper
│   └── AuditLogMapper.java
└── config
    └── AuditSecurityConfig.java        ← if separate security config needed
```

---

## 10. Internal Service API

The primary integration point for all other modules:

```
AuditService.log(AuditEventRequest request): void
```

`AuditEventRequest` contains:
- `actorUserId`: Long (nullable)
- `actionCode`: String (required)
- `resourceType`: String (nullable)
- `resourceId`: String (nullable)
- `module`: String (required)
- `ipAddress`: String (nullable)
- `details`: String — JSON string (nullable)
- `occurredAt`: LocalDateTime (defaults to `now()` if null)

This method must:
1. Never throw to the caller under any circumstance.
2. Capture the IP address from the request context if not explicitly provided.
3. Capture the current user ID from `SecurityContextUtils` if `actorUserId` is not provided.

A builder-style `AuditEventRequest.builder()` makes it convenient for callers to construct the request.

---

## 11. IP Address Capture

`AuditService` resolves the current request's IP address from the `HttpServletRequest` via `RequestContextHolder`. If no request context is active (e.g., scheduled job), `ipAddress` is left null.

`X-Forwarded-For` header is respected when the application is behind a reverse proxy (configurable: `attendai.audit.trust-proxy = true`).

---

## 12. API Contracts

Base path: `/api/v1/core/audit`

### GET /api/v1/core/audit/logs — Query Audit Log

**Permission:** `CORE_AUDIT_READ`

**Query params:**
| Param          | Description                           |
|----------------|---------------------------------------|
| `actorUserId`  | Filter by actor user ID               |
| `actionCode`   | Filter by action code (exact match)   |
| `resourceType` | Filter by resource type               |
| `resourceId`   | Filter by resource ID                 |
| `module`       | Filter by module                      |
| `fromDate`     | ISO datetime, inclusive               |
| `toDate`       | ISO datetime, inclusive               |
| `page`         | Page number (0-indexed)               |
| `size`         | Page size (default 20, max 100)       |

**Response 200:** Paginated `AuditLogResponse`
```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "actorUserId": 5,
      "actionCode": "AUTH_LOGIN_SUCCESS",
      "resourceType": "User",
      "resourceId": "5",
      "module": "core-auth",
      "ipAddress": "192.168.1.100",
      "details": null,
      "occurredAt": "2025-01-15T09:00:00Z"
    }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 342 }
}
```

---

### GET /api/v1/core/audit/logs/{id} — Get Single Audit Log Entry

**Permission:** `CORE_AUDIT_READ`

**Response 200:** `AuditLogResponse`
**Response 404:** Log entry not found

---

## 13. Authorization

| Operation           | Required Permission |
|---------------------|---------------------|
| Query audit log     | `CORE_AUDIT_READ`   |
| Get single entry    | `CORE_AUDIT_READ`   |
| Write audit event   | Internal Spring bean call only — no HTTP endpoint |

There is no public write API for audit logs. All writes go through the internal `AuditService.log()` Spring bean.

---

## 14. Configuration

| Property                            | Default | Description                                   |
|-------------------------------------|---------|-----------------------------------------------|
| `attendai.audit.trust-proxy`        | `false` | Trust `X-Forwarded-For` header for IP address |
| `attendai.audit.async`              | `false` | Write audit logs asynchronously (V1: false)   |
| `attendai.audit.max-details-length` | `10000` | Maximum length of the details JSON string     |

---

## 15. Integration Points

`core-audit` is a pure sink. It is called by every other module. It never calls any other module.

| Caller module     | Events written                                              |
|-------------------|-------------------------------------------------------------|
| `core-auth`       | Login, logout, token refresh, password reset                |
| `core-user`       | User create, update, status change, delete, password change |
| `core-role`       | Role CRUD, role assignment/removal                          |
| `core-permission` | Permission CRUD, permission assignment/removal              |
| `core-person`     | Person create, update, delete                               |
| `core-face`       | Face profile and image lifecycle, recognition events        |
| `core-attendance` | All attendance event state changes                          |
| `core-station`    | Station CRUD, status change, key regeneration               |
| `core-file`       | File upload, download, delete                               |
| `core-notification`| Notification permanently failed                            |
| Business modules  | Domain-specific events (school attendance, leave, etc.)     |

---

## 16. Error Handling

- `AuditService.log()` wraps all persistence logic in a try-catch. On failure, it logs at ERROR level with full exception detail. It never re-throws.
- If `AuditLogRepository.save()` fails (e.g., DB unavailable), the error is swallowed by the service but logged. The calling operation continues.
- Query operations throw `ResourceNotFoundException` for unknown IDs (standard pattern).

---

## 17. Performance and Scalability

- `audit_logs` is a write-heavy, read-occasionally table. Indexes are tuned for the most common query patterns.
- The `(actor_user_id, occurred_at)` composite index serves user activity history queries.
- The `(occurred_at)` index alone serves date-range queries.
- The `(resource_type, resource_id)` composite index serves resource history queries (e.g., "all events on this student").
- For V1, no archival strategy is defined. All records are retained in the live table.
- Future: partition `audit_logs` by month for databases exceeding 10M records.

---

## 18. Security Considerations

- Audit logs must never contain passwords, tokens, or raw PII in the `details` field. Callers are responsible for sanitizing details before calling `AuditService.log()`.
- The audit log is read-only via the API. No update or delete endpoint exists.
- `CORE_AUDIT_READ` permission must be restricted to system administrators only.
- Audit log data must not be included in general application backups that have wider access — audit data should be treated as sensitive operational data.

---

## 19. Flyway Migrations

```
V22__create_audit_logs_table.sql
```

---

## 20. Testing Strategy

| Test Type       | Scope                                                                |
|-----------------|----------------------------------------------------------------------|
| Unit — Service  | `log()` never throws; builds record correctly; IP/actor resolution   |
| Unit — Service  | `log()` catches persistence exceptions; logs error; continues       |
| Repository test | All filter combinations; date range; pagination; ordering           |
| Controller test | Query endpoint: filters, pagination, HTTP codes                      |
| Security tests  | `CORE_AUDIT_READ` required; no write endpoint exists                 |
| Integration     | `core-auth` login → audit record persisted and queryable             |

---

## 21. Implementation Roadmap

### Task 1: Entity, migration, repository
- `AuditLog` entity (no `BaseEntity` — custom, no `updatedAt` or `is_deleted`)
- `AuditLogRepository` with filter-based JPQL query
- Flyway: `V22__create_audit_logs_table.sql`

### Task 2: Service
- `AuditServiceImpl.log()`: build record, resolve IP, resolve actor, persist, never throw
- `AuditEventRequest` builder DTO

### Task 3: Controller and DTOs
- `AuditController.queryLogs()` and `getById()`
- `AuditLogFilter` request params, `AuditLogResponse` DTO
- `AuditLogMapper`

### Task 4: Integration with all Core modules
- Add `auditService.log(...)` calls to all service implementations across Core

---

## 22. Acceptance Criteria

- [ ] `AuditService.log()` never throws an exception to the caller
- [ ] Audit records are never updated or deleted via any code path
- [ ] `audit_logs` table has no `updated_at` or `is_deleted` column
- [ ] Every Core write operation produces an audit record
- [ ] Query by `actorUserId` returns all events for that user
- [ ] Query by date range returns only events within the range
- [ ] `CORE_AUDIT_READ` is required to query the log; no write endpoint exists
- [ ] Actor user ID is resolved automatically from the security context if not explicitly provided

---

## 23. Out of Scope

- Real-time audit streaming (WebSocket, SSE)
- Audit log archival or purging
- External SIEM integration (ELK, Splunk)
- Automatic PII detection or redaction in details
- Compliance reporting (SOC2, ISO27001 report generation)
