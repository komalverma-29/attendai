package com.attendai.core.audit.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Request object used by all modules to write an audit event.
 *
 * Use the builder to construct. All fields except {@code actionCode} and
 * {@code module} are optional — the {@link com.attendai.core.audit.service.AuditService}
 * resolves defaults from the security context when they are absent.
 */
@Getter
@Builder
public class AuditEventRequest {

    /** User who performed the action. Null for system-initiated actions. */
    private final Long actorUserId;

    /**
     * Machine-readable action code following the {@code <ENTITY>_<ACTION>} convention.
     * Example: {@code AUTH_LOGIN_SUCCESS}, {@code USER_CREATED}.
     * Required.
     */
    private final String actionCode;

    /** The entity type affected, e.g. "User", "AttendanceEvent". Optional. */
    private final String resourceType;

    /** String representation of the affected entity's ID. Optional. */
    private final String resourceId;

    /**
     * Module that generated this event, e.g. "core-auth", "school".
     * Required.
     */
    private final String module;

    /** IP address of the originating request. Resolved from context if null. */
    private final String ipAddress;

    /**
     * JSON string containing event-specific contextual data.
     * Must never contain passwords, tokens, or raw PII.
     * Optional.
     */
    private final String details;

    /**
     * When the event occurred. Defaults to {@code LocalDateTime.now()} when null.
     */
    @Builder.Default
    private final LocalDateTime occurredAt = LocalDateTime.now();
}
