-- V11: Create persons table
--
-- persons is a foundational table. All business-module entities link to it via FK.
-- Core never references business-module entities — FKs always point inward to persons.
--
-- Email uniqueness covers ALL rows including soft-deleted to prevent reuse.
-- The UNIQUE constraint on email applies across deleted and non-deleted rows.
--
-- Note on V3 (users): users.person_id exists but the FK constraint is added in V12
-- because persons did not exist when V3 was created.

CREATE TABLE persons (
    id                    BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    first_name            VARCHAR(100)     NOT NULL,
    middle_name           VARCHAR(100)     NULL,
    last_name             VARCHAR(100)     NOT NULL,
    gender                VARCHAR(20)      NULL,
    date_of_birth         DATE             NULL,
    email                 VARCHAR(255)     NULL,
    phone                 VARCHAR(30)      NULL,
    address_line1         VARCHAR(255)     NULL,
    address_line2         VARCHAR(255)     NULL,
    city                  VARCHAR(100)     NULL,
    state_or_province     VARCHAR(100)     NULL,
    postal_code           VARCHAR(20)      NULL,
    country               VARCHAR(2)       NULL,
    identity_doc_type     VARCHAR(30)      NULL,
    identity_doc_number   VARCHAR(100)     NULL,
    profile_photo_file_id BIGINT UNSIGNED  NULL,
    is_deleted            BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at            DATETIME         NULL,
    created_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            BIGINT UNSIGNED  NULL,
    updated_by            BIGINT UNSIGNED  NULL,

    UNIQUE uq_persons_email      (email),
    INDEX  idx_persons_last_name (last_name),
    INDEX  idx_persons_email     (email),
    INDEX  idx_persons_phone     (phone),
    INDEX  idx_persons_is_deleted(is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
