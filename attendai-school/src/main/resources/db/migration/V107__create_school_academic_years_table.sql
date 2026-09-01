-- V107: Create school_academic_years table
CREATE TABLE school_academic_years (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(100)     NOT NULL,
    start_date  DATE             NOT NULL,
    end_date    DATE             NOT NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'UPCOMING',
    description VARCHAR(500)     NULL,
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_academic_years_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    INDEX idx_school_academic_years_school (school_id),
    INDEX idx_school_academic_years_status (school_id, status),
    UNIQUE uq_school_academic_year_name (school_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
