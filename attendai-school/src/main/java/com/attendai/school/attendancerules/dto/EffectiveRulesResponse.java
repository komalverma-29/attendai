package com.attendai.school.attendancerules.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * The effective (merged) attendance rules for a section.
 *
 * <p>Section-level non-null override values take precedence over school-level values.
 * If no school-level rule set exists, core-config defaults are used.
 */
@Getter
@Builder
public class EffectiveRulesResponse {
    private final Long       schoolId;
    private final Long       academicYearId;
    private final Long       sectionId;
    private final LocalTime  lateThresholdTime;
    private final BigDecimal minAttendancePercentage;
    private final int        consecutiveAbsenceAlert;
    /**
     * True when the values come from a school-level rule set.
     * False when falling back to core-config defaults.
     */
    private final boolean    fromRuleSet;
}
