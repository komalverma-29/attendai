-- V112: Create school_subjects table
CREATE TABLE school_subjects (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(200)     NOT NULL,
    code        VARCHAR(20)      NOT NULL,
    type        VARCHAR(30)      NOT NULL,
    description VARCHAR(500)     NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_subjects_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_subjects_name (school_id, name),
    UNIQUE uq_school_subjects_code (school_id, code),
    INDEX  idx_school_subjects_school (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
