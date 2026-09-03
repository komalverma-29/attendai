package com.attendai.school.attendancerules.service;

import com.attendai.school.attendancerules.dto.AttendanceRuleSetResponse;
import com.attendai.school.attendancerules.dto.CreateRuleSetRequest;
import com.attendai.school.attendancerules.dto.CreateSectionOverrideRequest;
import com.attendai.school.attendancerules.dto.EffectiveRulesResponse;
import com.attendai.school.attendancerules.dto.SectionOverrideResponse;
import com.attendai.school.attendancerules.dto.UpdateRuleSetRequest;

import java.math.BigDecimal;
import java.time.LocalTime;

public interface AttendanceRulesService {

    // -------------------------------------------------------------------------
    // Rule set CRUD
    // -------------------------------------------------------------------------

    AttendanceRuleSetResponse createRuleSet(Long schoolId, Long academicYearId,
                                             CreateRuleSetRequest request);

    AttendanceRuleSetResponse getRuleSet(Long schoolId, Long academicYearId);

    AttendanceRuleSetResponse updateRuleSet(Long schoolId, Long academicYearId,
                                             UpdateRuleSetRequest request);

    // -------------------------------------------------------------------------
    // Section override management
    // -------------------------------------------------------------------------

    SectionOverrideResponse createSectionOverride(Long schoolId, Long academicYearId,
                                                   Long sectionId,
                                                   CreateSectionOverrideRequest request);

    void deleteSectionOverride(Long schoolId, Long academicYearId, Long sectionId);

    EffectiveRulesResponse getEffectiveRules(Long schoolId, Long academicYearId, Long sectionId);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-daily-attendance and school-attendance-reports
    // -------------------------------------------------------------------------

    /**
     * Returns the merged effective rules for a section.
     * Falls back to core-config defaults when no rule set exists.
     */
    EffectiveRulesResponse getEffectiveRules(Long sectionId, Long academicYearId);

    /**
     * Returns the effective late-arrival threshold for a section.
     * Used by attendance processing to classify LATE vs PRESENT.
     */
    LocalTime getLateThreshold(Long sectionId, Long academicYearId);

    /**
     * Returns the minimum attendance percentage for a school+year.
     * Used by attendance reports to flag shortage students.
     */
    BigDecimal getMinAttendancePercentage(Long schoolId, Long academicYearId);

    /**
     * Returns the consecutive-absence alert threshold for a section.
     * Used by attendance processing to trigger guardian notifications.
     */
    int getConsecutiveAbsenceAlert(Long sectionId, Long academicYearId);
}
