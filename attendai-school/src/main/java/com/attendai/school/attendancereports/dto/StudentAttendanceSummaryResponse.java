package com.attendai.school.attendancereports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Builder
public class StudentAttendanceSummaryResponse {
    private final Long       studentId;
    private final Long       academicYearId;
    private final LocalDate  fromDate;
    private final LocalDate  toDate;
    private final int        workingDays;
    private final int        presentDays;
    private final int        lateDays;
    private final int        absentDays;
    private final int        onLeaveDays;
    private final BigDecimal attendancePercentage;
    private final BigDecimal minimumRequired;
    private final boolean    belowThreshold;
}
