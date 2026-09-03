-- V123: Create school_teacher_assignments table
-- Highest existing migration: V122 (school_settings)
CREATE TABLE school_teacher_assignments (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    subject_id       BIGINT UNSIGNED  NOT NULL,
    teacher_id       BIGINT UNSIGNED  NOT NULL,
    is_class_teacher BOOLEAN          NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes            VARCHAR(500)     NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_ta_school  FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_ta_year    FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_ta_section FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_ta_subject FOREIGN KEY (subject_id)       REFERENCES school_subjects(id),
    CONSTRAINT fk_ta_teacher FOREIGN KEY (teacher_id)       REFERENCES school_teachers(id),
    UNIQUE uq_teacher_assignments   (section_id, subject_id, academic_year_id),
    INDEX  idx_ta_section_year      (section_id, academic_year_id),
    INDEX  idx_ta_teacher_year      (teacher_id, academic_year_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
