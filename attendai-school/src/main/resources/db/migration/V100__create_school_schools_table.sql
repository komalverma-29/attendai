-- V100: Create school_schools table
--
-- Root entity of the school module. All school-scoped entities hold a
-- school_id FK referencing this table.
-- School code is uppercase, 4–10 chars, unique, and immutable after creation.

CREATE TABLE school_schools (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255)     NOT NULL,
    code              VARCHAR(10)      NOT NULL,
    type              VARCHAR(20)      NOT NULL,
    status            VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    description       TEXT             NULL,
    address_line1     VARCHAR(255)     NOT NULL,
    address_line2     VARCHAR(255)     NULL,
    city              VARCHAR(100)     NOT NULL,
    state_or_province VARCHAR(100)     NOT NULL,
    postal_code       VARCHAR(20)      NULL,
    country           VARCHAR(2)       NOT NULL,
    phone             VARCHAR(30)      NULL,
    email             VARCHAR(255)     NULL,
    website           VARCHAR(500)     NULL,
    logo_file_id      BIGINT UNSIGNED  NULL,
    is_deleted        BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at        DATETIME         NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        BIGINT UNSIGNED  NULL,
    updated_by        BIGINT UNSIGNED  NULL,

    UNIQUE uq_school_schools_name   (name),
    UNIQUE uq_school_schools_code   (code),
    INDEX  idx_school_schools_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
