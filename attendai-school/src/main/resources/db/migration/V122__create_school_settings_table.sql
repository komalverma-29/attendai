-- V122: Create school_settings table
--
-- Stores school-level configuration overrides.
-- Each row overrides a specific setting key for a specific school.
-- No soft-delete — deleting a row resets the setting to its module default.
-- The UNIQUE constraint on (school_id, setting_key) enforces one value per key per school.

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
    UNIQUE uq_school_settings            (school_id, setting_key),
    INDEX  idx_school_settings_school    (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
