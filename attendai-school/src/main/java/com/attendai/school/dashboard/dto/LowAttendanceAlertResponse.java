package com.attendai.school.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LowAttendanceAlertResponse {
    private final Long       studentId;
    private final Long       sectionId;
    private final BigDecimal currentPercentage;
    private final BigDecimal threshold;
    private final BigDecimal shortfallPercentage;
}
