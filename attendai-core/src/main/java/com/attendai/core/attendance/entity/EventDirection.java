package com.attendai.core.attendance.entity;

/** Direction of an attendance event. Stored as VARCHAR — not a DB ENUM type. */
public enum EventDirection {
    /** Person is entering a location. */
    ENTRY,
    /** Person is exiting a location. */
    EXIT,
    /** Direction was not determined (e.g. bidirectional station without direction detection). */
    UNSPECIFIED
}
