-- V26: Create face_images table
--
-- Stores enrolled face images for a profile.
-- The raw image binary is stored in core-file (referenced by file_id).
-- embedding_vector is a JSON array of floats extracted by the recognition engine.
-- SECURITY: embedding_vector must never be exposed in any API response.

CREATE TABLE face_images (
    id               BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    face_profile_id  BIGINT UNSIGNED  NOT NULL,
    file_id          BIGINT UNSIGNED  NOT NULL,
    embedding_vector TEXT             NOT NULL,
    captured_at      DATETIME         NULL,
    is_deleted       BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at       DATETIME         NULL,
    created_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by       BIGINT UNSIGNED  NULL,
    updated_by       BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_face_images_profile FOREIGN KEY (face_profile_id) REFERENCES face_profiles(id),
    INDEX idx_face_images_profile_id (face_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
