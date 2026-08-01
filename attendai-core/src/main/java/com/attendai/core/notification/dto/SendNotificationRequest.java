package com.attendai.core.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request object used by all modules to dispatch a notification.
 *
 * The notification service resolves the template, renders content, checks
 * user preferences, and dispatches via the configured channels.
 */
@Getter
@Builder
public class SendNotificationRequest {

    /** The user to notify. */
    private final Long recipientUserId;

    /**
     * Notification type code that maps to a template.
     * Example: "AUTH_PASSWORD_RESET", "SCHOOL_STUDENT_ABSENT"
     */
    private final String typeCode;

    /**
     * Channels to attempt delivery on.
     * Example: ["EMAIL", "IN_APP"]
     * If null or empty, the notification service uses its configured defaults.
     */
    private final java.util.List<String> channels;

    /**
     * Template variable values keyed by placeholder name.
     * Example: {"firstName": "John", "resetLink": "https://..."}
     */
    private final Map<String, String> variables;

    /**
     * When to send the notification. Null means send immediately.
     */
    private final LocalDateTime scheduledAt;

    /** Locale for template selection. Defaults to "en" when null. */
    @Builder.Default
    private final String locale = "en";
}
