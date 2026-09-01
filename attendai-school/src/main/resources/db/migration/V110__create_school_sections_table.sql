-- V110: Create school_sections table
-- Also seeds the missing SCHOOL_SECTION_DELETE permission (was absent from V102).
INSERT IGNORE INTO permissions (code, name, module, description, is_system, is_deleted) VALUES
('SCHOOL_SECTION_DELETE', 'Delete Section', 'SCHOOL', 'Soft-delete a section', FALSE, FALSE);

CREATE TABLE school_sections (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    class_id         BIGINT UNSIGNED  NOT NULL,
    academic_year_id BIGINT UNSIGNED  NOT NULL,
    name             VARCHAR(50)      NOT NULL,
    description      VARCHAR(255)     NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_sections_school FOREIGN KEY (school_id)        REFERENCES school_schools(id),
    CONSTRAINT fk_school_sections_class  FOREIGN KEY (class_id)         REFERENCES school_classes(id),
    CONSTRAINT fk_school_sections_year   FOREIGN KEY (academic_year_id) REFERENCES school_academic_years(id),
    UNIQUE     uq_school_sections_name   (class_id, academic_year_id, name),
    INDEX      idx_school_sections_class_year (class_id, academic_year_id),
    INDEX      idx_school_sections_school     (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
