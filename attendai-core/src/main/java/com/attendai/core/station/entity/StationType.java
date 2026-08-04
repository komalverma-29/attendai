package com.attendai.core.station.entity;

/**
 * Classifies what direction(s) of attendance events a station records.
 * Stored as VARCHAR — not a DB ENUM type.
 */
public enum StationType {

    /** Records entry (check-in) events only. */
    ENTRY,

    /** Records exit (check-out) events only. */
    EXIT,

    /** Records both entry and exit; the device determines the direction. */
    ENTRY_EXIT,

    /** Administrator manually records events through this station. */
    MANUAL
}
