-- V128: Create school_daily_attendance table
-- One record per student per date (enforced by unique constraint).
-- No soft-delete: attendance records are overridden in-place, not deleted.
CREATE TABLE school_daily_attendance (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    student_id       BIGINT UNSIGNED  NOT NULL,
    attendance_date  DATE             NOT NULL,
    status           VARCHAR(20)      NOT NULL,
    arrival_time     TIME             NULL,
    core_event_id    BIGINT UNSIGNED  NULL,
    remarks          VARCHAR(500)     NULL,
    marked_by_id     BIGINT UNSIGNED  NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_daily_att_school  FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_daily_att_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_daily_att_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_daily_att_student FOREIGN KEY (student_id)       REFERENCES school_students(id),
    UNIQUE uq_daily_att_student_date  (student_id, attendance_date),
    INDEX  idx_daily_att_section_date (section_id, attendance_date),
    INDEX  idx_daily_att_student_date (student_id, attendance_date),
    INDEX  idx_daily_att_school_date  (school_id, attendance_date),
    INDEX  idx_daily_att_status       (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
