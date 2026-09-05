package com.attendai.school.attendancereports.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class AttendanceShortageResponse {
    private final Long       studentId;
    private final Long       sectionId;
    private final BigDecimal attendancePercentage;
    private final BigDecimal minimumRequired;
    private final BigDecimal shortfallPercentage;
}
