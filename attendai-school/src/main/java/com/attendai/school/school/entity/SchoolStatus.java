package com.attendai.school.school.entity;

/**
 * Operational status of a school.
 * Stored as VARCHAR — not a DB ENUM type.
 *
 * Status transitions:
 *   ACTIVE    → INACTIVE    (admin deactivates)
 *   ACTIVE    → SUSPENDED   (platform admin)
 *   INACTIVE  → ACTIVE      (admin activates)
 *   SUSPENDED → ACTIVE      (platform admin)
 */
public enum SchoolStatus {
    /** Fully operational. Attendance, enrollment, and timetabling are permitted. */
    ACTIVE,
    /** Manually deactivated. No new operations permitted. */
    INACTIVE,
    /** Platform-level suspension. No operations permitted. */
    SUSPENDED;

    /** Returns true when this status allows attendance and enrollment operations. */
    public boolean isOperational() {
        return this == ACTIVE;
    }
}
