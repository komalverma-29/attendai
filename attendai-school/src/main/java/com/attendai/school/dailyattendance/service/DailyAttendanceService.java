package com.attendai.school.dailyattendance.service;

import com.attendai.school.dailyattendance.dto.DailyAttendanceRecordResponse;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.dto.SectionAttendanceSummaryResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;

import java.time.LocalDate;
import java.util.List;

public interface DailyAttendanceService {

    // -------------------------------------------------------------------------
    // Scheduler entry points (called by scheduled jobs)
    // -------------------------------------------------------------------------

    /** FR-DA-01: processes a batch of PENDING core-attendance events for a school. */
    void processSchoolAttendanceEvents(Long schoolId);

    /** FR-DA-02: marks students with no record as ABSENT for today (for a school). */
    void runMarkAbsentJob(Long schoolId);

    // -------------------------------------------------------------------------
    // HTTP-facing queries
    // -------------------------------------------------------------------------

    SectionAttendanceSummaryResponse getSectionAttendanceForDate(
            Long schoolId, Long sectionId, LocalDate date);

    List<DailyAttendanceRecordResponse> getStudentAttendance(
            Long schoolId, Long studentId, LocalDate fromDate, LocalDate toDate,
            Long academicYearId);

    DailyAttendanceRecordResponse overrideAttendance(
            Long schoolId, Long recordId, OverrideAttendanceRequest request, Long actorUserId);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-leave and future modules
    // -------------------------------------------------------------------------

    /**
     * Creates or updates the record for the student on the given date to ON_LEAVE.
     * Called by school-leave when a leave application is approved.
     */
    void setOnLeave(Long studentId, LocalDate date, Long schoolId, Long academicYearId,
                    Long sectionId);

    /**
     * Resets an ON_LEAVE record to ABSENT (or re-derives from core events if available).
     * Called by school-leave when approved leave is revoked.
     */
    void resetOnLeave(Long studentId, LocalDate date, Long schoolId);

    /**
     * Returns the status for a student on a date, or null if no record exists.
     */
    DailyAttendanceStatus getStatus(Long studentId, LocalDate date);

    /**
     * Returns true if a record exists for the student on the date.
     */
    boolean hasRecord(Long studentId, LocalDate date);
}
