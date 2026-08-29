-- V18: Create in_app_notifications table
CREATE TABLE in_app_notifications (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT UNSIGNED  NOT NULL,
    type_code         VARCHAR(100)     NOT NULL,
    title             VARCHAR(255)     NOT NULL,
    body              TEXT             NOT NULL,
    is_read           BOOLEAN          NOT NULL DEFAULT FALSE,
    read_at           DATETIME         NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_in_app_notifications_recipient (recipient_user_id),
    INDEX idx_in_app_notifications_unread    (recipient_user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
