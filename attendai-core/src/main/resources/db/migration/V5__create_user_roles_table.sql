-- V5: Create user_roles join table
CREATE TABLE user_roles (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED  NOT NULL,
    role_id     BIGINT UNSIGNED  NOT NULL,
    assigned_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE uq_user_roles           (user_id, role_id),
    INDEX  idx_user_roles_user_id  (user_id),
    INDEX  idx_user_roles_role_id  (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
