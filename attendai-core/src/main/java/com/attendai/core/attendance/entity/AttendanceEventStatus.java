package com.attendai.core.attendance.entity;

/**
 * Lifecycle state of an attendance event.
 *
 * State machine:
 *   [Received] → PENDING
 *   PENDING    → PROCESSED  (business module calls markAsProcessed)
 *   PENDING    → DUPLICATE  (deduplication window hit on receipt)
 *   PENDING    → REJECTED   (validation failure at time of recording)
 *   PROCESSED  → [immutable]
 *   REJECTED   → [immutable]
 *   DUPLICATE  → [immutable]
 */
public enum AttendanceEventStatus {
    /** Recorded, awaiting consumption by a business module. */
    PENDING,
    /** Consumed and processed by a business module. */
    PROCESSED,
    /** Rejected at recording time — station inactive, unknown person, or time constraint violated. */
    REJECTED,
    /** Flagged as a duplicate within the configured deduplication window. */
    DUPLICATE
}
