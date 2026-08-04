-- V15: Create attendance_events table
--
-- This is the highest-volume table in the system.
-- Index strategy is optimised for three critical patterns:
--   1. Deduplication:  (person_id, station_id, direction, event_time)
--   2. Person history: (person_id, event_time)
--   3. Pending poll:   (status)
--
-- No soft-delete (is_deleted column) — attendance events are never deleted.
-- Events in PROCESSED, REJECTED, DUPLICATE status are immutable.
--
-- originalEventId is a self-referencing FK for correction chains.

CREATE TABLE attendance_events (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    person_id         BIGINT UNSIGNED  NOT NULL,
    station_id        BIGINT UNSIGNED  NULL,
    event_time        DATETIME         NOT NULL,
    direction         VARCHAR(20)      NOT NULL DEFAULT 'UNSPECIFIED',
    source            VARCHAR(30)      NOT NULL,
    status            VARCHAR(20)      NOT NULL DEFAULT 'PENDING',
    rejection_reason  VARCHAR(500)     NULL,
    notes             VARCHAR(500)     NULL,
    original_event_id BIGINT UNSIGNED  NULL,
    processed_at      DATETIME         NULL,
    processed_by      VARCHAR(50)      NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by        BIGINT UNSIGNED  NULL,
    updated_by        BIGINT UNSIGNED  NULL,

    CONSTRAINT fk_att_events_person   FOREIGN KEY (person_id)         REFERENCES persons(id),
    CONSTRAINT fk_att_events_station  FOREIGN KEY (station_id)        REFERENCES stations(id),
    CONSTRAINT fk_att_events_original FOREIGN KEY (original_event_id) REFERENCES attendance_events(id),

    INDEX idx_att_events_person_id    (person_id),
    INDEX idx_att_events_station_id   (station_id),
    INDEX idx_att_events_event_time   (event_time),
    INDEX idx_att_events_status       (status),
    INDEX idx_att_events_person_time  (person_id, event_time),
    INDEX idx_att_events_dedup        (person_id, station_id, direction, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
