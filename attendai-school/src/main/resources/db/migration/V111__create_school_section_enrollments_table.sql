-- V111: Create school_section_enrollments table
-- Enrollments are NOT soft-deleted; removal is a hard delete.
CREATE TABLE school_section_enrollments (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    section_id       BIGINT UNSIGNED  NOT NULL,
    student_id       BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    roll_number      VARCHAR(20)      NOT NULL,
    enrolled_at      DATE             NOT NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_enrollments_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id)       REFERENCES school_students(id),
    CONSTRAINT fk_enrollments_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE uq_enrollments_student_year  (student_id, academic_year_id),
    UNIQUE uq_enrollments_roll_number   (section_id, academic_year_id, roll_number),
    INDEX  idx_enrollments_section      (section_id),
    INDEX  idx_enrollments_student      (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
