-- V109: Create school_classes table
-- Note: V108 is intentionally unassigned per the school module specification.
CREATE TABLE school_classes (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id    BIGINT UNSIGNED  NOT NULL,
    name         VARCHAR(100)     NOT NULL,
    display_name VARCHAR(100)     NULL,
    grade_order  INT              NOT NULL,
    status       VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted   BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME         NULL,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_classes_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_classes_name    (school_id, name),
    INDEX  idx_school_classes_school (school_id),
    INDEX  idx_school_classes_order  (school_id, grade_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
