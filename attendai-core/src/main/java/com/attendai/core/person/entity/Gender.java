package com.attendai.core.person.entity;

/**
 * Gender options for a person.
 * Stored as VARCHAR in the database (not as a DB ENUM type).
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}
