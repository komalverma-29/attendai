package com.attendai.school.dailyattendance.dto;

import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/** Lightweight per-student view used in the section attendance list. */
@Getter
@Builder
public class StudentAttendanceDayResponse {
    private final Long                  studentId;
    private final String                rollNumber;
    private final DailyAttendanceStatus status;
    private final LocalTime             arrivalTime;
    private final String                remarks;
}
