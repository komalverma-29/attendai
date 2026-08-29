-- V17: Create notification_logs table
CREATE TABLE notification_logs (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT UNSIGNED  NOT NULL,
    type_code         VARCHAR(100)     NOT NULL,
    channel           VARCHAR(20)      NOT NULL,
    status            VARCHAR(30)      NOT NULL DEFAULT 'PENDING',
    subject           VARCHAR(255)     NULL,
    rendered_body     TEXT             NULL,
    error_message     VARCHAR(1000)    NULL,
    attempt_count     INT              NOT NULL DEFAULT 0,
    scheduled_at      DATETIME         NULL,
    sent_at           DATETIME         NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_notification_logs_recipient  (recipient_user_id),
    INDEX idx_notification_logs_status     (status),
    INDEX idx_notification_logs_type_code  (type_code),
    INDEX idx_notification_logs_scheduled  (status, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
