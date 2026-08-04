package com.attendai.core.attendance.entity;

/** How an attendance event was captured. Stored as VARCHAR — not a DB ENUM type. */
public enum EventSource {
    /** Identified via face recognition engine. */
    FACE_RECOGNITION,
    /** Identified via QR code scan. */
    QR_CODE,
    /** Manually recorded by an operator. Bypasses deduplication. */
    MANUAL,
    /** Submitted via API by an external system. */
    API,
    /** Correction of a previous event. Always creates a new PENDING event. */
    CORRECTION
}
