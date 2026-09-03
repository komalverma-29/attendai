-- V126: Create school_attendance_rule_sets table
-- One rule set per school+academic-year (enforced by unique constraint).
-- No soft delete — rule sets are updated in-place or deleted definitively.
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
