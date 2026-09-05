package com.attendai.school.attendancereports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class SchoolAttendanceOverviewResponse {
    private final Long       schoolId;
    private final LocalDate  fromDate;
    private final LocalDate  toDate;
    private final int        totalStudents;
    private final int        presentCount;
    private final int        lateCount;
    private final int        absentCount;
    private final int        onLeaveCount;
    private final BigDecimal attendancePercentage;
}
