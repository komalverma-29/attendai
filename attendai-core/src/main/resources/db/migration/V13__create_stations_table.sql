-- V13: Create stations table
--
-- api_key_hash stores the SHA-256 hex digest of the raw API key.
-- The raw key is never persisted — it is returned once at creation/regeneration.
-- The UNIQUE constraint on api_key_hash enables O(1) station authentication lookups.

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

    UNIQUE uq_stations_name          (name),
    UNIQUE uq_stations_api_key_hash  (api_key_hash),
    INDEX  idx_stations_status       (status),
    INDEX  idx_stations_type         (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
