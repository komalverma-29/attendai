-- V23: Create system_configs table
--
-- Stores runtime-configurable key-value settings for the platform.
-- Keys are stored in lowercase and follow the <module>.<category>.<name> convention.
-- Values are always strings; typed access is provided by ConfigService.
--
-- SECURITY: This table must never store secrets, passwords, or tokens.
-- Use environment variables for sensitive configuration.

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

    UNIQUE uq_system_configs_key    (config_key),
    INDEX  idx_system_configs_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
