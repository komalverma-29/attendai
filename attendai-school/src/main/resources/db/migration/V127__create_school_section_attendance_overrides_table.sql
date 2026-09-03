-- V127: Create school_section_attendance_overrides table
-- Section-level overrides for a rule set.
-- NULL column values mean "use school-level value" (merge-on-read pattern).
CREATE TABLE school_section_attendance_overrides (
    id                        BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    rule_set_id               BIGINT UNSIGNED  NOT NULL,
    section_id                BIGINT UNSIGNED  NOT NULL,
    late_threshold_time       TIME             NULL,
    min_attendance_percentage DECIMAL(5,2)     NULL,
    consecutive_absence_alert INT              NULL,
    created_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by                BIGINT UNSIGNED  NULL,
    updated_by                BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_overrides_rule_set FOREIGN KEY (rule_set_id) REFERENCES school_attendance_rule_sets(id),
    CONSTRAINT fk_overrides_section  FOREIGN KEY (section_id)  REFERENCES school_sections(id),
    UNIQUE uq_section_overrides (rule_set_id, section_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
