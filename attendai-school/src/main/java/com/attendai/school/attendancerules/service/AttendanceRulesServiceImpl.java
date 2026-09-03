package com.attendai.school.attendancerules.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancerules.dto.AttendanceRuleSetResponse;
import com.attendai.school.attendancerules.dto.CreateRuleSetRequest;
import com.attendai.school.attendancerules.dto.CreateSectionOverrideRequest;
import com.attendai.school.attendancerules.dto.EffectiveRulesResponse;
import com.attendai.school.attendancerules.dto.SectionOverrideResponse;
import com.attendai.school.attendancerules.dto.UpdateRuleSetRequest;
import com.attendai.school.attendancerules.entity.AttendanceRuleSet;
import com.attendai.school.attendancerules.entity.SectionAttendanceRuleOverride;
import com.attendai.school.attendancerules.exception.AttendanceRuleSetNotFoundException;
import com.attendai.school.attendancerules.mapper.AttendanceRulesMapper;
import com.attendai.school.attendancerules.repository.AttendanceRuleSetRepository;
import com.attendai.school.attendancerules.repository.SectionAttendanceRuleOverrideRepository;
import com.attendai.school.section.service.SchoolSectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceRulesServiceImpl implements AttendanceRulesService {

    // Defaults from BR-RULES-04, BR-RULES-05, BR-RULES-06
    static final LocalTime  DEFAULT_LATE_THRESHOLD       = LocalTime.of(9, 0);
    static final BigDecimal DEFAULT_MIN_PERCENTAGE        = new BigDecimal("75.00");
    static final int        DEFAULT_CONSECUTIVE_ALERT     = 3;

    private static final String MODULE = "school";

    private final AttendanceRuleSetRepository            ruleSetRepository;
    private final SectionAttendanceRuleOverrideRepository overrideRepository;
    private final AttendanceRulesMapper                  rulesMapper;
    private final AcademicYearService                    academicYearService;
    private final SchoolSectionService                   sectionService;
    private final AuditService                           auditService;

    // =========================================================================
    // Rule set CRUD
    // =========================================================================

    @Override
    @Transactional
    public AttendanceRuleSetResponse createRuleSet(Long schoolId, Long academicYearId,
                                                    CreateRuleSetRequest request) {
        // Validate academic year belongs to this school
        academicYearService.findById(schoolId, academicYearId); // throws if not found or wrong school

        // BR-RULES-01: one rule set per school+year
        if (ruleSetRepository.existsBySchoolIdAndAcademicYearId(schoolId, academicYearId)) {
            throw new ResourceAlreadyExistsException(
                    "An attendance rule set already exists for school " + schoolId
                    + " and academic year " + academicYearId);
        }

        AttendanceRuleSet ruleSet = AttendanceRuleSet.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .lateThresholdTime(request.getLateThresholdTime())
                .minAttendancePercentage(request.getMinAttendancePercentage())
                .consecutiveAbsenceAlert(request.getConsecutiveAbsenceAlert())
                .build();

        AttendanceRuleSet saved = ruleSetRepository.save(ruleSet);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_RULE_SET_CREATED")
                .module(MODULE).resourceType("AttendanceRuleSet")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"academicYearId\":" + academicYearId + "}")
                .build());

        return rulesMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceRuleSetResponse getRuleSet(Long schoolId, Long academicYearId) {
        return rulesMapper.toResponse(requireRuleSet(schoolId, academicYearId));
    }

    @Override
    @Transactional
    public AttendanceRuleSetResponse updateRuleSet(Long schoolId, Long academicYearId,
                                                    UpdateRuleSetRequest request) {
        AttendanceRuleSet ruleSet = requireRuleSet(schoolId, academicYearId);

        if (request.getLateThresholdTime() != null) {
            ruleSet.setLateThresholdTime(request.getLateThresholdTime());
        }
        if (request.getMinAttendancePercentage() != null) {
            ruleSet.setMinAttendancePercentage(request.getMinAttendancePercentage());
        }
        if (request.getConsecutiveAbsenceAlert() != null) {
            ruleSet.setConsecutiveAbsenceAlert(request.getConsecutiveAbsenceAlert());
        }

        AttendanceRuleSet saved = ruleSetRepository.save(ruleSet);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_RULE_SET_UPDATED")
                .module(MODULE).resourceType("AttendanceRuleSet")
                .resourceId(String.valueOf(saved.getId())).build());

        return rulesMapper.toResponse(saved);
    }

    // =========================================================================
    // Section override management
    // =========================================================================

    @Override
    @Transactional
    public SectionOverrideResponse createSectionOverride(Long schoolId, Long academicYearId,
                                                          Long sectionId,
                                                          CreateSectionOverrideRequest request) {
        // Validate section belongs to this school
        var section = sectionService.findById(sectionId);
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Section " + sectionId + " does not belong to school " + schoolId);
        }

        // The rule set must already exist before an override can be created
        AttendanceRuleSet ruleSet = requireRuleSet(schoolId, academicYearId);

        // BR-RULES-02: at most one override per section per rule set
        if (overrideRepository.existsByRuleSetIdAndSectionId(ruleSet.getId(), sectionId)) {
            throw new ResourceAlreadyExistsException(
                    "An override already exists for section " + sectionId
                    + " in this rule set. Update it by re-posting.");
        }

        SectionAttendanceRuleOverride override = SectionAttendanceRuleOverride.builder()
                .ruleSetId(ruleSet.getId())
                .sectionId(sectionId)
                .lateThresholdTime(request.getLateThresholdTime())
                .minAttendancePercentage(request.getMinAttendancePercentage())
                .consecutiveAbsenceAlert(request.getConsecutiveAbsenceAlert())
                .build();

        SectionAttendanceRuleOverride saved = overrideRepository.save(override);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_RULE_OVERRIDE_CREATED")
                .module(MODULE).resourceType("SectionAttendanceRuleOverride")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"sectionId\":" + sectionId + "}")
                .build());

        return rulesMapper.toOverrideResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSectionOverride(Long schoolId, Long academicYearId, Long sectionId) {
        AttendanceRuleSet ruleSet = requireRuleSet(schoolId, academicYearId);

        if (!overrideRepository.existsByRuleSetIdAndSectionId(ruleSet.getId(), sectionId)) {
            throw new com.attendai.core.common.exception.ResourceNotFoundException(
                    "No override found for section " + sectionId
                    + " in rule set " + ruleSet.getId());
        }

        overrideRepository.deleteByRuleSetIdAndSectionId(ruleSet.getId(), sectionId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_RULE_OVERRIDE_DELETED")
                .module(MODULE).resourceType("SectionAttendanceRuleOverride")
                .resourceId("section-" + sectionId)
                .details("{\"schoolId\":" + schoolId + ",\"sectionId\":" + sectionId + "}")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveRulesResponse getEffectiveRules(Long schoolId, Long academicYearId,
                                                     Long sectionId) {
        // Validate section belongs to this school
        var section = sectionService.findById(sectionId);
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Section " + sectionId + " does not belong to school " + schoolId);
        }
        return computeEffectiveRules(schoolId, academicYearId, sectionId);
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public EffectiveRulesResponse getEffectiveRules(Long sectionId, Long academicYearId) {
        var section = sectionService.findById(sectionId);
        Long schoolId = section.getSchoolId();
        return computeEffectiveRules(schoolId, academicYearId, sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalTime getLateThreshold(Long sectionId, Long academicYearId) {
        return getEffectiveRules(sectionId, academicYearId).getLateThresholdTime();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getMinAttendancePercentage(Long schoolId, Long academicYearId) {
        return ruleSetRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId)
                .map(AttendanceRuleSet::getMinAttendancePercentage)
                .orElse(DEFAULT_MIN_PERCENTAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public int getConsecutiveAbsenceAlert(Long sectionId, Long academicYearId) {
        return getEffectiveRules(sectionId, academicYearId).getConsecutiveAbsenceAlert();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AttendanceRuleSet requireRuleSet(Long schoolId, Long academicYearId) {
        return ruleSetRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId)
                .orElseThrow(() -> new AttendanceRuleSetNotFoundException(schoolId, academicYearId));
    }

    /**
     * Core merge logic (FR-RULES-06 / section 6 of SDD):
     * <ol>
     *   <li>Load school-level rule set for (schoolId, academicYearId)</li>
     *   <li>Load section override if one exists</li>
     *   <li>Merge: section non-null values override school-level values</li>
     *   <li>If no school-level rule set: use built-in defaults</li>
     * </ol>
     */
    private EffectiveRulesResponse computeEffectiveRules(Long schoolId, Long academicYearId,
                                                          Long sectionId) {
        var ruleSetOpt = ruleSetRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId);

        // Base values — school-level or built-in defaults
        LocalTime  baseLate        = DEFAULT_LATE_THRESHOLD;
        BigDecimal basePercentage  = DEFAULT_MIN_PERCENTAGE;
        int        baseAlert       = DEFAULT_CONSECUTIVE_ALERT;
        boolean    fromRuleSet     = false;

        if (ruleSetOpt.isPresent()) {
            AttendanceRuleSet rs = ruleSetOpt.get();
            baseLate       = rs.getLateThresholdTime();
            basePercentage = rs.getMinAttendancePercentage();
            baseAlert      = rs.getConsecutiveAbsenceAlert();
            fromRuleSet    = true;

            // Apply section override if one exists
            var overrideOpt = overrideRepository
                    .findByRuleSetIdAndSectionId(rs.getId(), sectionId);
            if (overrideOpt.isPresent()) {
                SectionAttendanceRuleOverride ov = overrideOpt.get();
                if (ov.getLateThresholdTime()         != null) baseLate       = ov.getLateThresholdTime();
                if (ov.getMinAttendancePercentage()   != null) basePercentage = ov.getMinAttendancePercentage();
                if (ov.getConsecutiveAbsenceAlert()   != null) baseAlert      = ov.getConsecutiveAbsenceAlert();
            }
        }

        return EffectiveRulesResponse.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .sectionId(sectionId)
                .lateThresholdTime(baseLate)
                .minAttendancePercentage(basePercentage)
                .consecutiveAbsenceAlert(baseAlert)
                .fromRuleSet(fromRuleSet)
                .build();
    }
}
