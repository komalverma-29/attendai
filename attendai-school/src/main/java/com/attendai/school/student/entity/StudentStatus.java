package com.attendai.school.student.entity;

/** Lifecycle status of a student enrollment. Stored as VARCHAR. */
public enum StudentStatus {
    ACTIVE,
    INACTIVE,
    /** Student has moved to another school. Terminal state. */
    TRANSFERRED,
    /** Student has completed their schooling. Terminal state. */
    GRADUATED
}
