package com.attendai.school.attendancerules.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class CreateRuleSetRequest {

    @NotNull(message = "Late threshold time is required")
    private LocalTime lateThresholdTime;

    @NotNull(message = "Minimum attendance percentage is required")
    @DecimalMin(value = "0.00",   message = "Percentage must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Percentage must be between 0 and 100")
    private BigDecimal minAttendancePercentage;

    @Min(value = 1, message = "Consecutive absence alert must be at least 1")
    private int consecutiveAbsenceAlert = 3;
}
