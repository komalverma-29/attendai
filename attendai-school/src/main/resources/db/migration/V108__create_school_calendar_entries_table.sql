-- V108: Create school_calendar_entries table
-- Calendar entries are hard-deleted (no soft delete) because the absence of
-- an entry has semantic meaning: a missing entry defaults to the weekday/weekend rule.
CREATE TABLE school_calendar_entries (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    entry_date       DATE             NOT NULL,
    entry_type       VARCHAR(20)      NOT NULL,
    name             VARCHAR(200)     NOT NULL,
    description      VARCHAR(500)     NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_cal_entries_school FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_cal_entries_year   FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_cal_entries_date      (school_id, academic_year_id, entry_date),
    INDEX  idx_cal_entries_year     (school_id, academic_year_id),
    INDEX  idx_cal_entries_date_range (school_id, academic_year_id, entry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
