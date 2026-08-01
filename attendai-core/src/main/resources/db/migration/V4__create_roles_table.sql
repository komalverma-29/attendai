-- V4: Create roles table
CREATE TABLE roles (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(100)     NOT NULL,
    name        VARCHAR(255)     NOT NULL,
    description TEXT             NULL,
    is_system   BOOLEAN          NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    UNIQUE uq_roles_code     (code),
    INDEX  idx_roles_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
