package com.attendai.school.attendancerules.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Section-level override request.
 * All fields are optional (null = use school-level rule).
 */
@Getter
@Setter
public class CreateSectionOverrideRequest {

    /** Null → use school-level late threshold. */
    private LocalTime lateThresholdTime;

    /** Null → use school-level min attendance percentage. */
    @DecimalMin(value = "0.00",   message = "Percentage must be between 0 and 100")
    @DecimalMax(value = "100.00", message = "Percentage must be between 0 and 100")
    private BigDecimal minAttendancePercentage;

    /** Null → use school-level consecutive absence alert. */
    @Min(value = 1, message = "Consecutive absence alert must be at least 1")
    private Integer consecutiveAbsenceAlert;
}
