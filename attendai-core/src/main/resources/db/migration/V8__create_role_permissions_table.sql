-- V8: Create role_permissions join table
CREATE TABLE role_permissions (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT UNSIGNED  NOT NULL,
    permission_id BIGINT UNSIGNED  NOT NULL,
    assigned_at   DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by   BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles(id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions(id),
    UNIQUE uq_role_permissions                (role_id, permission_id),
    INDEX  idx_role_permissions_role_id       (role_id),
    INDEX  idx_role_permissions_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
