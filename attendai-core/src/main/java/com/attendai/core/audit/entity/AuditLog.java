package com.attendai.core.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable audit log record.
 *
 * <p>This entity intentionally does NOT extend {@link com.attendai.core.common.entity.BaseEntity}
 * because audit records must never be updated. There is no {@code updated_at},
 * no {@code is_deleted}, and no {@code created_by} / {@code updated_by} audit trail
 * on the audit log itself — the audit log IS the trail.
 *
 * <p>The table is append-only. No service method updates or deletes rows.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** User who performed the action; null for system-initiated actions. */
    @Column(name = "actor_user_id")
    private Long actorUserId;

    /** Machine-readable action code, e.g. AUTH_LOGIN_SUCCESS. */
    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    /** Entity type affected, e.g. "User". */
    @Column(name = "resource_type", length = 100)
    private String resourceType;

    /** String ID of the affected entity. */
    @Column(name = "resource_id", length = 100)
    private String resourceId;

    /** Module that wrote the event. */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** IP address of the originating request. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** JSON string with event-specific context data. */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** When the event occurred (UTC). */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** When this record was inserted. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
