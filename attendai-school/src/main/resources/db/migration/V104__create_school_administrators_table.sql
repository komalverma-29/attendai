-- V104: Create school_administrators table
--
-- Links a Core Person + Core User to a school as an administrator.
-- A school must always have at least one ACTIVE administrator.
-- Unique constraints prevent one person being admin in multiple schools
-- and one user being linked to multiple administrator records.

CREATE TABLE school_administrators (
    id           BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id    BIGINT UNSIGNED  NOT NULL,
    person_id    BIGINT UNSIGNED  NOT NULL,
    user_id      BIGINT UNSIGNED  NOT NULL,
    designation  VARCHAR(100)     NULL,
    status       VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    notes        VARCHAR(500)     NULL,
    is_deleted   BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at   DATETIME         NULL,
    created_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by   BIGINT UNSIGNED  NULL,
    updated_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_school_admins_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_school_admins_person_school (person_id, school_id),
    UNIQUE uq_school_admins_user          (user_id),
    INDEX  idx_school_admins_school_id    (school_id),
    INDEX  idx_school_admins_status       (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
