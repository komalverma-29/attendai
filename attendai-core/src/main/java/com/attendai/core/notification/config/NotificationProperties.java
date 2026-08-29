package com.attendai.core.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Notification module configuration properties.
 * Bound from {@code application.yml} under the prefix {@code attendai.notification}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.notification")
public class NotificationProperties {

    /** Enable the email channel. Default: true. */
    private boolean emailEnabled = true;

    /** Enable the push channel. Default: false (V1 stub only). */
    private boolean pushEnabled = false;

    /** Maximum retry attempts for failed notification delivery. Default: 3. */
    private int retryMaxAttempts = 3;

    /** Retry job interval in seconds. Default: 300 (5 minutes). */
    private long retryIntervalSeconds = 300L;

    /** Sender email address. Required when email is enabled. */
    private String emailFrom = "noreply@attendai.app";

    /** Optional reply-to email address. */
    private String emailReplyTo;
}
