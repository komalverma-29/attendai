package com.attendai.core.station.entity;

/**
 * Lifecycle state of an attendance station.
 * Stored as VARCHAR — not a DB ENUM type.
 *
 * Allowed transitions:
 * ACTIVE → INACTIVE | MAINTENANCE
 * INACTIVE → ACTIVE
 * MAINTENANCE → ACTIVE
 */
public enum StationStatus {

    /** Fully operational. Can accept heartbeats and attendance events. */
    ACTIVE,

    /** Deactivated. Cannot accept heartbeats or attendance events. */
    INACTIVE,

    /** Temporarily under maintenance. Can accept heartbeats but not attendance events. */
    MAINTENANCE;

    /** Returns true when this station can accept attendance events. */
    public boolean canAcceptEvents() {
        return this == ACTIVE;
    }

    /** Returns true when this station can accept heartbeats. */
    public boolean canReceiveHeartbeat() {
        return this == ACTIVE || this == MAINTENANCE;
    }
}
