package com.attendai.school.attendancerules.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AttendanceRuleSetResponse {
    private final Long        id;
    private final Long        schoolId;
    private final Long        academicYearId;
    private final LocalTime   lateThresholdTime;
    private final BigDecimal  minAttendancePercentage;
    private final int         consecutiveAbsenceAlert;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
