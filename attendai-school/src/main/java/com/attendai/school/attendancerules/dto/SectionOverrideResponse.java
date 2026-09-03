package com.attendai.school.attendancerules.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class SectionOverrideResponse {
    private final Long        id;
    private final Long        ruleSetId;
    private final Long        sectionId;
    /** Null means this field uses the school-level value. */
    private final LocalTime   lateThresholdTime;
    /** Null means this field uses the school-level value. */
    private final BigDecimal  minAttendancePercentage;
    /** Null means this field uses the school-level value. */
    private final Integer     consecutiveAbsenceAlert;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
