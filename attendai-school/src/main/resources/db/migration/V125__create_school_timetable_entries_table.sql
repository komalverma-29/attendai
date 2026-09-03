-- V125: Create school_timetable_entries table
CREATE TABLE school_timetable_entries (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    section_id       BIGINT UNSIGNED  NOT NULL,
    time_slot_id     BIGINT UNSIGNED  NOT NULL,
    day_of_week      VARCHAR(10)      NOT NULL,
    assignment_id    BIGINT UNSIGNED  NOT NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_tt_school     FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_tt_year       FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    CONSTRAINT fk_tt_section    FOREIGN KEY (section_id)       REFERENCES school_sections(id),
    CONSTRAINT fk_tt_slot       FOREIGN KEY (time_slot_id)     REFERENCES school_time_slots(id),
    CONSTRAINT fk_tt_assignment FOREIGN KEY (assignment_id)    REFERENCES school_teacher_assignments(id),
    UNIQUE uq_timetable_slot       (section_id, time_slot_id, day_of_week, academic_year_id),
    UNIQUE uq_timetable_teacher    (assignment_id, time_slot_id, day_of_week),
    INDEX  idx_timetable_section   (section_id, academic_year_id),
    INDEX  idx_timetable_assignment (assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
