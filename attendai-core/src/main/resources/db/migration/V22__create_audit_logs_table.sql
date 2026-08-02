-- V22: Create audit_logs table
--
-- Audit records are immutable and append-only.
-- This table intentionally has NO updated_at, NO is_deleted, NO created_by/updated_by.
-- The audit log IS the trail — it does not audit itself.
--
-- actor_user_id has NO FK constraint. Audit records must survive even if the
-- referenced user is soft-deleted or hard-deleted in the future.
--
-- Index strategy is optimised for the most common query patterns:
--   - actor history:     (actor_user_id, occurred_at)
--   - date range:        (occurred_at)
--   - resource history:  (resource_type, resource_id)
--   - action filtering:  (action_code)
--   - module filtering:  (module)

CREATE TABLE audit_logs (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT UNSIGNED  NULL,
    action_code   VARCHAR(100)     NOT NULL,
    resource_type VARCHAR(100)     NULL,
    resource_id   VARCHAR(100)     NULL,
    module        VARCHAR(50)      NOT NULL,
    ip_address    VARCHAR(45)      NULL,
    details       TEXT             NULL,
    occurred_at   DATETIME         NOT NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_logs_actor       (actor_user_id),
    INDEX idx_audit_logs_action_code (action_code),
    INDEX idx_audit_logs_resource    (resource_type, resource_id),
    INDEX idx_audit_logs_occurred_at (occurred_at),
    INDEX idx_audit_logs_module      (module),
    INDEX idx_audit_logs_actor_date  (actor_user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
