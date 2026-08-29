-- V19: Create notification_preferences table
CREATE TABLE notification_preferences (
    id         BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED  NOT NULL,
    type_code  VARCHAR(100)     NOT NULL,
    channel    VARCHAR(20)      NOT NULL,
    is_enabled BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_prefs_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE uq_notification_preferences   (user_id, type_code, channel),
    INDEX  idx_notification_prefs_user   (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
