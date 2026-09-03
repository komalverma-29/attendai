-- V124: Create school_time_slots table
CREATE TABLE school_time_slots (
    id          BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(50)      NOT NULL,
    start_time  TIME             NOT NULL,
    end_time    TIME             NOT NULL,
    slot_order  INT              NOT NULL,
    slot_type   VARCHAR(20)      NOT NULL DEFAULT 'PERIOD',
    is_deleted  BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME         NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT UNSIGNED  NULL,
    updated_by  BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_time_slots_school FOREIGN KEY (school_id) REFERENCES school_schools(id),
    UNIQUE uq_time_slots_name    (school_id, name),
    INDEX  idx_time_slots_school (school_id, slot_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
