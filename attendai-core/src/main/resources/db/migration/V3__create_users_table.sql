-- V3: Create users table
--
-- persons table (V2) must exist before this migration runs.
-- The FK fk_users_person references persons(id).
-- Email is UNIQUE across ALL users including soft-deleted ones (prevent reuse).
-- Username is UNIQUE — enforced at DB level; soft-delete is filtered in app.

CREATE TABLE users (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    person_id            BIGINT UNSIGNED  NOT NULL,
    email                VARCHAR(255)     NOT NULL,
    username             VARCHAR(50)      NOT NULL,
    password_hash        VARCHAR(255)     NOT NULL,
    status               VARCHAR(20)      NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN          NOT NULL DEFAULT TRUE,
    last_login_at        DATETIME         NULL,
    is_deleted           BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at           DATETIME         NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    UNIQUE  uq_users_email    (email),
    UNIQUE  uq_users_username (username),
    INDEX   idx_users_person_id (person_id),
    INDEX   idx_users_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
