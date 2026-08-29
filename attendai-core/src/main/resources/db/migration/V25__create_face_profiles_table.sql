-- V25: Create face_profiles table
--
-- One person may have at most one non-deleted face profile.
-- image_count is a denormalized count — maintained by FaceServiceImpl.

CREATE TABLE face_profiles (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    person_id   BIGINT UNSIGNED  NOT NULL,
    status      VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    image_count INT              NOT NULL DEFAULT 0,
    notes       TEXT             NULL,
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_face_profiles_person FOREIGN KEY (person_id) REFERENCES persons(id),
    INDEX idx_face_profiles_person_id (person_id),
    INDEX idx_face_profiles_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
