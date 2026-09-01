-- V106: Create school_students table
CREATE TABLE school_students (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id        BIGINT UNSIGNED  NOT NULL,
    person_id        BIGINT UNSIGNED  NOT NULL,
    user_id          BIGINT UNSIGNED  NULL,
    admission_number VARCHAR(50)      NOT NULL,
    status           VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    blood_group      VARCHAR(5)       NULL,
    guardian_name    VARCHAR(200)     NULL,
    guardian_phone   VARCHAR(30)      NULL,
    guardian_email   VARCHAR(255)     NULL,
    enrollment_date  DATE             NOT NULL,
    notes            VARCHAR(500)     NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_students_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_students_person_school (person_id, school_id),
    UNIQUE uq_school_students_admission     (school_id, admission_number),
    UNIQUE uq_school_students_user          (user_id),
    INDEX  idx_school_students_school_id    (school_id),
    INDEX  idx_school_students_status       (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
