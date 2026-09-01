-- V105: Create school_teachers table
CREATE TABLE school_teachers (
    id             BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id      BIGINT UNSIGNED  NOT NULL,
    person_id      BIGINT UNSIGNED  NOT NULL,
    user_id        BIGINT UNSIGNED  NULL,
    employee_code  VARCHAR(50)      NULL,
    designation    VARCHAR(100)     NULL,
    qualification  VARCHAR(255)     NULL,
    department     VARCHAR(100)     NULL,
    status         VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes          VARCHAR(500)     NULL,
    joining_date   DATE             NULL,
    is_deleted     BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at     DATETIME         NULL,
    created_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by     BIGINT UNSIGNED  NULL,
    updated_by     BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_teachers_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_teachers_person_school   (person_id, school_id),
    UNIQUE uq_school_teachers_employee_code   (school_id, employee_code),
    UNIQUE uq_school_teachers_user            (user_id),
    INDEX  idx_school_teachers_school_id      (school_id),
    INDEX  idx_school_teachers_status         (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
