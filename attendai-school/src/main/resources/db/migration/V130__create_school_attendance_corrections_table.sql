-- V130: Create school_attendance_corrections table
-- Note: SDD specifies V120, but V122 is already applied; next available is V130.
CREATE TABLE school_attendance_corrections (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id            BIGINT UNSIGNED  NOT NULL,
    academic_year_id     BIGINT UNSIGNED  NOT NULL,
    student_id           BIGINT UNSIGNED  NOT NULL,
    attendance_record_id BIGINT UNSIGNED  NOT NULL,
    attendance_date      DATE             NOT NULL,
    original_status      VARCHAR(20)      NOT NULL,
    requested_status     VARCHAR(20)      NOT NULL,
    reason               VARCHAR(1000)    NOT NULL,
    evidence_file_id     BIGINT UNSIGNED  NULL,
    status               VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    requested_by_id      BIGINT UNSIGNED  NOT NULL,
    reviewed_by_id       BIGINT UNSIGNED  NULL,
    reviewed_at          DATETIME         NULL,
    rejection_reason     VARCHAR(1000)    NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_corrections_school  FOREIGN KEY (school_id)            REFERENCES school_schools(id),
    CONSTRAINT fk_corrections_year    FOREIGN KEY (academic_year_id)     REFERENCES school_academic_years(id),
    CONSTRAINT fk_corrections_student FOREIGN KEY (student_id)           REFERENCES school_students(id),
    CONSTRAINT fk_corrections_record  FOREIGN KEY (attendance_record_id) REFERENCES school_daily_attendance(id),
    INDEX idx_corrections_school  (school_id),
    INDEX idx_corrections_student (student_id),
    INDEX idx_corrections_status  (status),
    INDEX idx_corrections_date    (attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
