-- V2: Create password_reset_tokens table
--
-- Stores hashed, single-use password reset tokens.
-- Raw tokens are never stored. The token_hash column is the lookup key.

CREATE TABLE password_reset_tokens (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED  NOT NULL,
    token_hash  VARCHAR(64)      NOT NULL,
    expires_at  DATETIME         NOT NULL,
    is_used     BOOLEAN          NOT NULL DEFAULT FALSE,
    used_at     DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    UNIQUE  uq_password_reset_tokens_token_hash  (token_hash),
    INDEX   idx_password_reset_tokens_user_id    (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
