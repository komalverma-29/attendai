-- V14: Create station_configs table
--
-- Stores per-station configuration overrides.
-- These take precedence over system-level defaults from system_configs
-- when computing the effective configuration for a station.

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
    UNIQUE uq_station_configs            (station_id, config_key),
    INDEX  idx_station_configs_station_id (station_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
