package com.attendai.core.notification.entity;

/**
 * Delivery status of a notification log entry.
 * Stored as VARCHAR — not a DB ENUM type.
 */
public enum NotificationStatus {
    /** Queued for dispatch (includes scheduled future notifications). */
    PENDING,
    /** Successfully delivered. */
    SENT,
    /** Delivery attempt failed; will be retried. */
    FAILED,
    /** Skipped due to user preference or missing template. */
    SKIPPED,
    /** Max retries exhausted; will not be retried further. */
    PERMANENTLY_FAILED
}
