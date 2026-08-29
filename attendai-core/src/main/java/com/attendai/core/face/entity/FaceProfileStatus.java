package com.attendai.core.face.entity;

/**
 * Lifecycle state of a face profile.
 * Stored as VARCHAR — not a DB ENUM type.
 *
 * State machine:
 *   [Created] → PENDING
 *   PENDING   → ACTIVE       (activate, requires ≥1 image)
 *   ACTIVE    → INACTIVE     (deactivate)
 *   INACTIVE  → ACTIVE       (re-activate, requires ≥1 image)
 *   ACTIVE    → PENDING      (last image removed)
 *   Any       → [Deleted]    (soft delete)
 */
public enum FaceProfileStatus {
    /** Profile created; no images enrolled yet. Cannot be used for recognition. */
    PENDING,
    /** At least one image enrolled; ready for face recognition. */
    ACTIVE,
    /** Temporarily disabled; person will not be matched during recognition. */
    INACTIVE
}
