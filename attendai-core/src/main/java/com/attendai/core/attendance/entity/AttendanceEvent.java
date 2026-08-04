package com.attendai.core.attendance.entity;

import com.attendai.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single attendance event — the atomic unit recorded when a person's presence
 * is detected or manually noted at an attendance station.
 *
 * <p>This entity is deliberately domain-agnostic. Business modules (school, college,
 * enterprise) consume PENDING events and apply their own domain-specific rules.
 *
 * <p>Intentionally does NOT extend {@link com.attendai.core.common.entity.SoftDeletableEntity} —
 * attendance events are never soft-deleted. They are immutable once they reach
 * PROCESSED, REJECTED, or DUPLICATE status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_events")
public class AttendanceEvent extends BaseEntity {

    /** FK → persons(id). Always required. */
    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    /**
     * FK → stations(id). Nullable for MANUAL and API sources.
     * For station-sourced events this is set from the authenticated station context.
     */
    @Column(name = "station_id", updatable = false)
    private Long stationId;

    /** When the attendance was recorded (UTC). */
    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    @Builder.Default
    private EventDirection direction = EventDirection.UNSPECIFIED;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30, updatable = false)
    private EventSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceEventStatus status = AttendanceEventStatus.PENDING;

    /** Reason why this event was rejected. Null for non-rejected events. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Optional operator notes. */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * FK → attendance_events(id). Set only when source = CORRECTION.
     * Allows chaining: correction of a correction creates a traceable chain.
     */
    @Column(name = "original_event_id", updatable = false)
    private Long originalEventId;

    /** When a business module marked this event as processed. */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Identifier of the business module that processed this event, e.g. "school".
     * Set when a module calls markAsProcessed.
     */
    @Column(name = "processed_by", length = 50)
    private String processedBy;
}
