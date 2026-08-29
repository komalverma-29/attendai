-- V16: Create notification_templates table
CREATE TABLE notification_templates (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    type_code     VARCHAR(100)     NOT NULL,
    channel       VARCHAR(20)      NOT NULL,
    locale        VARCHAR(10)      NOT NULL DEFAULT 'en',
    subject       VARCHAR(255)     NULL,
    body_template TEXT             NOT NULL,
    is_active     BOOLEAN          NOT NULL DEFAULT TRUE,
    is_deleted    BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME         NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    BIGINT UNSIGNED  NULL,
    updated_by    BIGINT UNSIGNED  NULL,

    UNIQUE uq_notification_templates         (type_code, channel, locale),
    INDEX  idx_notification_templates_type   (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
