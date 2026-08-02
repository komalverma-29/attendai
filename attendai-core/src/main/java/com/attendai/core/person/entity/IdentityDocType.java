package com.attendai.core.person.entity;

/**
 * Identity document types.
 * Stored as VARCHAR in the database (not as a DB ENUM type).
 */
public enum IdentityDocType {
    PASSPORT,
    NATIONAL_ID,
    DRIVING_LICENCE,
    OTHER
}
