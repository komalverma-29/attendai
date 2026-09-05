package com.attendai.school.dailyattendance.entity;

/** Status of a student's daily attendance record. Stored as VARCHAR. */
public enum DailyAttendanceStatus {
    /** Student arrived on time. */
    PRESENT,
    /** Student arrived after the late threshold. */
    LATE,
    /** Student did not arrive; no event recorded by mark-absent time. */
    ABSENT,
    /** Student was on approved leave (set by school-leave on approval). */
    ON_LEAVE
}
