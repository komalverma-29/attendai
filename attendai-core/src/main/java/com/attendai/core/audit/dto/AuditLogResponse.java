package com.attendai.core.audit.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Read-only response DTO for a single audit log entry.
 *
 * Audit records are immutable — this DTO exposes all stored fields
 * but never exposes a write path.
 */
@Getter
@Builder
public class AuditLogResponse {

    private final Long          id;
    private final Long          actorUserId;
    private final String        actionCode;
    private final String        resourceType;
    private final String        resourceId;
    private final String        module;
    private final String        ipAddress;
    private final String        details;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdAt;
}
