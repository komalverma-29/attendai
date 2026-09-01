package com.attendai.school.school.entity;

/** Classifies the grade range a school covers. Stored as VARCHAR. */
public enum SchoolType {
    /** Grades 1–5. */
    PRIMARY,
    /** Grades 6–10. */
    SECONDARY,
    /** Grades 11–12. */
    HIGHER_SECONDARY,
    /** Grades 1–12 (full school). */
    COMBINED
}
