-- V1: Create refresh_tokens table
--
-- Raw refresh tokens are never stored. Only the SHA-256 hex hash is persisted.
-- The token_hash column is the lookup key (UNIQUE index for O(1) lookups).

CREATE TABLE refresh_tokens (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT UNSIGNED  NOT NULL,
    token_hash  VARCHAR(64)      NOT NULL,
    expires_at  DATETIME         NOT NULL,
    is_revoked  BOOLEAN          NOT NULL DEFAULT FALSE,
    revoked_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    UNIQUE  uq_refresh_tokens_token_hash       (token_hash),
    INDEX   idx_refresh_tokens_user_id         (user_id),
    INDEX   idx_refresh_tokens_user_active     (user_id, is_revoked, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
