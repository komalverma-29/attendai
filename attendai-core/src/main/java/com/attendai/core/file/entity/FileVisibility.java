package com.attendai.core.file.entity;

/**
 * Controls whether a file is accessible without authentication.
 * Stored as VARCHAR — not a DB ENUM type.
 */
public enum FileVisibility {

    /** Only the uploader and users with CORE_FILE_READ can access. */
    PRIVATE,

    /**
     * Accessible without authentication.
     * Must only be used for genuinely public content (logos, icons).
     * Never store personal or sensitive data as PUBLIC.
     */
    PUBLIC
}
