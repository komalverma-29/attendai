-- V113: Create school_class_subjects table (class-subject associations)
CREATE TABLE school_class_subjects (
    id         BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    class_id   BIGINT UNSIGNED  NOT NULL,
    subject_id BIGINT UNSIGNED  NOT NULL,
    created_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_class_subjects_class   FOREIGN KEY (class_id)   REFERENCES school_classes(id),
    CONSTRAINT fk_class_subjects_subject FOREIGN KEY (subject_id) REFERENCES school_subjects(id),
    UNIQUE uq_class_subjects         (class_id, subject_id),
    INDEX  idx_class_subjects_class  (class_id),
    INDEX  idx_class_subjects_subject (subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
