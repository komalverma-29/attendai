# Specification: core-config

## 1. Overview

`core-config` is the system configuration key-value store for the AttendAI platform. It provides a runtime-configurable store of named settings that any module can read at runtime — without redeployment.

Unlike `application.yml` which stores static, deployment-time configuration, `core-config` stores configuration that administrators need to change while the application is running: thresholds, feature flags, default behaviour values, and operational parameters.

`core-config` is consumed by business modules and other Core modules via the `ConfigService` Spring bean.

---

## 2. Scope and Objectives

**In scope:**
- System configuration key-value storage and retrieval
- Typed value retrieval (String, Integer, Boolean, BigDecimal)
- Default value fallback when a key is not set
- Configuration key namespacing by module
- Read/write API for administrators
- In-memory caching of configuration values with TTL-based invalidation
- Auditing of configuration changes
- Seeding of default system configuration values on startup

**Out of scope:**
- Secret/credential storage (use environment variables)
- Per-user configuration (use `notification_preferences` or domain-specific settings)
- Per-station overrides (managed by `core-station` as `station_configs`)
- Feature flag framework with rollout percentages or user targeting

---

## 3. Functional Requirements

### FR-CONFIG-01: Set Configuration Value
Create or update a configuration key-value pair. If the key already exists, it is overwritten. Every write is audit-logged.

### FR-CONFIG-02: Get Configuration Value
Retrieve the value for a given key. Returns a typed value. If the key does not exist and a default is provided, the default is returned. If no default is provided, a `ConfigKeyNotFoundException` is thrown.

### FR-CONFIG-03: Get All Configuration for a Module
Return all configuration entries belonging to a given module namespace.

### FR-CONFIG-04: Delete Configuration Key
Remove a configuration key. After deletion, reads for that key return the application default (or throw if no default is registered).

### FR-CONFIG-05: List All Configuration
Return a paginated list of all configuration entries, filterable by module and optionally by key pattern.

### FR-CONFIG-06: Startup Seeding
On application startup, seed default configuration values if the keys do not already exist. Existing values are not overwritten.

### FR-CONFIG-07: Cached Read
Configuration reads are served from an in-memory cache. The cache is invalidated on write or after a configurable TTL (default: 60 seconds).

---

## 4. Non-Functional Requirements

- Configuration reads must be served from cache (< 1ms per read in the hot path).
- Configuration writes must invalidate the cache immediately.
- The `configs` table is small — expected to have fewer than 1,000 entries even in a large installation.
- Configuration keys follow the naming convention `<module>.<category>.<name>`, e.g. `attendance.dedup.window-seconds`, `face.recognition.threshold`.
- Configuration values are stored as strings. Typed accessors in `ConfigService` handle conversion.

---

## 5. Business Rules

- BR-CONFIG-01: Configuration keys follow the dot-separated naming convention `<module>.<category>.<name>`.
- BR-CONFIG-02: Key names are case-insensitive in lookups (stored lowercase).
- BR-CONFIG-03: Secrets (passwords, API keys) must never be stored in `core-config`. Use environment variables.
- BR-CONFIG-04: Startup seeding does not overwrite existing keys. It only inserts missing defaults.
- BR-CONFIG-05: A configuration write always produces an audit log entry.
- BR-CONFIG-06: Value length is capped at 1,000 characters.

---

## 6. Domain Model

### SystemConfig Entity

| Field       | Type          | Description                                              |
|-------------|---------------|----------------------------------------------------------|
| id          | Long          | Surrogate PK                                             |
| configKey   | String        | Unique key, lowercase, max 200                           |
| configValue | String        | Value (always stored as string), max 1000                |
| module      | String        | Module that owns this key, max 50                        |
| description | String        | Human-readable description, max 500                      |
| isEncrypted | boolean       | Reserved for future encrypted value support; always false in V1 |
| createdAt   | LocalDateTime | Audit                                                    |
| updatedAt   | LocalDateTime | Audit                                                    |
| createdBy   | Long          | Audit                                                    |
| updatedBy   | Long          | Audit                                                    |

---

## 7. Data Model

### Table: `system_configs`

```sql
CREATE TABLE system_configs (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(200)     NOT NULL,
    config_value VARCHAR(1000)    NOT NULL,
    module       VARCHAR(50)      NOT NULL,
    description  VARCHAR(500)     NULL,
    is_encrypted BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    UNIQUE uq_system_configs_key (config_key),
    INDEX idx_system_configs_module (module)
);
```

---

## 8. ConfigService Internal API

This is the primary interface consumed by all other modules as a Spring bean:

```
ConfigService.getString(String key): String
ConfigService.getString(String key, String defaultValue): String
ConfigService.getInt(String key): int
ConfigService.getInt(String key, int defaultValue): int
ConfigService.getBoolean(String key): boolean
ConfigService.getBoolean(String key, boolean defaultValue): boolean
ConfigService.getBigDecimal(String key): BigDecimal
ConfigService.getBigDecimal(String key, BigDecimal defaultValue): BigDecimal
ConfigService.set(String key, String value, String module): void
ConfigService.delete(String key): void
```

Type conversion failures (e.g., a key that is expected to be an integer but has a non-numeric value) throw `ConfigValueConversionException`.

---

## 9. Default System Configuration Keys

These keys are seeded on startup by `core-config`:

| Key                                      | Module       | Default Value | Description                                    |
|------------------------------------------|--------------|---------------|------------------------------------------------|
| `attendance.dedup.window-seconds`        | `attendance` | `300`         | Deduplication window in seconds                |
| `attendance.event.max-future-seconds`    | `attendance` | `60`          | Max seconds an event can be in the future      |
| `attendance.event.max-past-hours`        | `attendance` | `24`          | Max hours an event can be backdated            |
| `face.recognition.threshold`             | `face`       | `0.85`        | Minimum confidence for a positive match        |
| `face.recognition.max-images-per-profile`| `face`       | `10`          | Maximum face images per profile                |
| `face.liveness.enabled`                  | `face`       | `false`       | Enable liveness detection globally             |
| `notification.retry.max-attempts`        | `notification`| `3`          | Max notification delivery retry attempts       |
| `notification.retry.interval-seconds`   | `notification`| `300`        | Notification retry scheduler interval          |
| `file.max-size-bytes`                    | `file`       | `10485760`    | Maximum file upload size (10 MB)               |
| `security.max-sessions`                  | `auth`       | `5`           | Max concurrent refresh tokens per user         |
| `security.reset-token-expiry-seconds`    | `auth`       | `3600`        | Password reset token TTL in seconds            |

Business modules register their own keys during their startup seeding phase. Core does not define business module keys.

---

## 10. Package Organization

```
com.attendai.core.config
├── entity
│   └── SystemConfig.java
├── repository
│   └── SystemConfigRepository.java
├── service
│   ├── ConfigService.java
│   └── ConfigServiceImpl.java
├── cache
│   └── ConfigCache.java
├── controller
│   └── ConfigController.java
├── dto
│   ├── SetConfigRequest.java
│   ├── SystemConfigResponse.java
│   └── SystemConfigSummaryResponse.java
├── mapper
│   └── SystemConfigMapper.java
├── seeder
│   └── CoreConfigSeeder.java
└── exception
    ├── ConfigKeyNotFoundException.java
    └── ConfigValueConversionException.java
```

---

## 11. Cache Strategy

`ConfigCache` is an in-memory `ConcurrentHashMap<String, String>` held in a Spring `@Component` singleton:

- On first read of a key: check cache → if miss, load from DB → store in cache → return
- On `set()`: write to DB → immediately update cache
- On `delete()`: delete from DB → remove from cache
- TTL-based full cache invalidation: a `@Scheduled` job clears the cache every 60 seconds (configurable), forcing a reload from DB on next read

This simple strategy is sufficient for V1 with a small config table (< 1,000 entries) and moderate write frequency (config values rarely change in production).

---

## 12. API Contracts

Base path: `/api/v1/core/config`

### GET /api/v1/core/config — List All Config

**Permission:** `CORE_CONFIG_READ`

**Query params:** `module`, `search` (key pattern), `page`, `size`

**Response 200:** Paginated `SystemConfigSummaryResponse`
```json
{
  "success": true,
  "data": [
    {
      "configKey": "face.recognition.threshold",
      "configValue": "0.85",
      "module": "face",
      "description": "Minimum confidence for a positive match"
    }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 12 }
}
```

---

### GET /api/v1/core/config/{key} — Get Config by Key

**Permission:** `CORE_CONFIG_READ`

**Response 200:** `SystemConfigResponse`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "configKey": "face.recognition.threshold",
    "configValue": "0.85",
    "module": "face",
    "description": "Minimum confidence for a positive face match",
    "updatedAt": "2025-01-10T08:00:00Z"
  }
}
```

**Response 404:** Key not found

---

### PUT /api/v1/core/config/{key} — Set Config Value

**Permission:** `CORE_CONFIG_WRITE`

**Request:**
```json
{
  "value": "0.90",
  "module": "face",
  "description": "Raised recognition threshold for tighter security"
}
```

**Response 200:** `SystemConfigResponse`

---

### DELETE /api/v1/core/config/{key} — Delete Config Key

**Permission:** `CORE_CONFIG_WRITE`

**Response 204**

---

## 13. Validation Rules

### SetConfigRequest
| Field       | Rule                                           |
|-------------|------------------------------------------------|
| value       | Not blank, max 1000                            |
| module      | Not blank, max 50, lowercase + dots/hyphens    |
| description | Optional, max 500                              |

Key format validation (applied at service layer):
- Lowercase
- Dot-separated segments
- Letters, digits, hyphens only in each segment

---

## 14. Authorization

| Operation          | Required Permission  |
|--------------------|----------------------|
| List/read config   | `CORE_CONFIG_READ`   |
| Set/delete config  | `CORE_CONFIG_WRITE`  |
| Internal `get*()`  | Spring bean — no HTTP auth required |

---

## 15. Integration Points

`core-config` provides configuration values to all other modules. It calls no other module.

| Module that reads config | Keys consumed                                     |
|--------------------------|---------------------------------------------------|
| `core-attendance`        | `attendance.dedup.window-seconds`, event time limits |
| `core-face`              | `face.recognition.threshold`, liveness, max-images |
| `core-auth`              | `security.max-sessions`, `security.reset-token-expiry-seconds` |
| `core-notification`      | Retry settings                                    |
| `core-file`              | `file.max-size-bytes`                             |
| `core-station`           | Station config overrides via `station_configs` read `core-config` for defaults |
| Business modules         | Domain-specific keys they seed themselves         |

---

## 16. Error Handling

| Scenario                      | Exception                       | HTTP / Behaviour |
|-------------------------------|---------------------------------|------------------|
| Key not found (no default)    | `ConfigKeyNotFoundException`    | 404 (API), throw (internal) |
| Value type conversion fails   | `ConfigValueConversionException`| 500 (internal — programming error) |
| Write validation failure      | `ValidationException`           | 400              |

---

## 17. Logging and Audit

| Action             | Audit Code        | Details                       |
|--------------------|-------------------|-------------------------------|
| Config key set     | `CONFIG_UPDATED`  | key, old_value, new_value     |
| Config key deleted | `CONFIG_DELETED`  | key                           |

`old_value` is captured before overwrite for audit purposes.

Service-level logging:
- INFO: Key set, key deleted
- DEBUG: Cache hit/miss
- WARN: Key not found when no default provided (internal caller)

---

## 18. Security Considerations

- `CORE_CONFIG_WRITE` must be restricted to platform administrators only.
- Configuration values must never contain secrets, tokens, or passwords.
- The description field is informational only and is visible in the API response — no sensitive data should be placed there.
- Audit logging captures old and new values for all writes. For any value that looks like a secret (contains `password`, `token`, `secret`, `key` in the config key name), the log entry should mask the value as `[REDACTED]`. This is a safety check, not a substitute for the rule against storing secrets.

---

## 19. Flyway Migrations

```
V23__create_system_configs_table.sql
V24__seed_core_system_configs.sql
```

---

## 20. Testing Strategy

| Test Type       | Scope                                                              |
|-----------------|--------------------------------------------------------------------|
| Unit — Service  | `getString`, `getInt`, `getBoolean`, `getBigDecimal` — hit, miss, default |
| Unit — Service  | Type conversion failure throws `ConfigValueConversionException`   |
| Unit — Cache    | Cache hit bypasses DB; write invalidates cache; TTL invalidation  |
| Unit — Seeder   | Seed inserts missing keys; does not overwrite existing            |
| Repository test | `findByConfigKey`, `findByModule`                                 |
| Controller test | List, get by key, set, delete — HTTP codes, validation            |
| Security tests  | Read requires `CORE_CONFIG_READ`; write requires `CORE_CONFIG_WRITE` |
| Integration     | Set key → `core-attendance` reads new value from service bean     |

---

## 21. Implementation Roadmap

### Task 1: Entity, migration, repository
- `SystemConfig` entity extending `BaseEntity`
- `SystemConfigRepository`
- Flyway: `V23__create_system_configs_table.sql`

### Task 2: Cache
- `ConfigCache` with `ConcurrentHashMap`
- TTL invalidation via `@Scheduled`

### Task 3: Service
- `ConfigServiceImpl`: all typed getters with cache-first logic
- `set()` and `delete()` with cache invalidation
- `ConfigKeyNotFoundException` and `ConfigValueConversionException`

### Task 4: Controller and DTOs
- `ConfigController` with all endpoints
- `SystemConfigMapper`, request/response DTOs

### Task 5: Startup seeding
- `CoreConfigSeeder` — `ApplicationRunner` that seeds default keys on startup
- Flyway: `V24__seed_core_system_configs.sql` (optional SQL seed as alternative)

### Task 6: Audit integration
- Write audit events on set and delete

---

## 22. Acceptance Criteria

- [ ] `ConfigService.getString(key, default)` returns the default when the key is absent
- [ ] `ConfigService.getString(key)` throws `ConfigKeyNotFoundException` when absent with no default
- [ ] Reads hit the in-memory cache; writes go to DB and invalidate the cache
- [ ] `set()` does not insert duplicate keys — it upserts
- [ ] Startup seeding inserts default keys if absent; does not overwrite existing values
- [ ] Every write produces an audit log entry capturing old and new values
- [ ] `CORE_CONFIG_WRITE` is required to set or delete keys
- [ ] Config key names containing `password`, `token`, `secret`, or `key` are masked as `[REDACTED]` in audit logs

---

## 23. Out of Scope

- Secret / credential storage
- Per-user configuration
- Feature flag rollout percentages or user targeting
- Configuration history / versioning (only the current value is stored)
- Distributed cache (Redis) — in-memory cache is sufficient for V1 single-node deployment
