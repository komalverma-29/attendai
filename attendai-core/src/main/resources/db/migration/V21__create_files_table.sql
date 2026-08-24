-- V21: Create files table
--
-- Stores file metadata. The actual binary content lives in the storage backend
-- (local filesystem or S3-compatible object store).
--
-- storage_key is the internal path — UNIQUE per file, NEVER exposed via API.
-- uploaded_by_user_id has no FK constraint (file records must survive user deletion).

CREATE TABLE files (
    id                   BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    original_name        VARCHAR(255)     NOT NULL,
    storage_key          VARCHAR(500)     NOT NULL,
    content_type         VARCHAR(100)     NOT NULL,
    size_bytes           BIGINT UNSIGNED  NOT NULL,
    visibility           VARCHAR(10)      NOT NULL DEFAULT 'PRIVATE',
    uploaded_by_user_id  BIGINT UNSIGNED  NOT NULL,
    module               VARCHAR(50)      NULL,
    is_deleted           BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at           DATETIME         NULL,
    created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by           BIGINT UNSIGNED  NULL,
    updated_by           BIGINT UNSIGNED  NULL,

    UNIQUE uq_files_storage_key     (storage_key),
    INDEX  idx_files_uploaded_by    (uploaded_by_user_id),
    INDEX  idx_files_module         (module),
    INDEX  idx_files_is_deleted     (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
