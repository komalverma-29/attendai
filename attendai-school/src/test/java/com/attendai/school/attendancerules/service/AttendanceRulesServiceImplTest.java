package com.attendai.school.attendancerules.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancerules.dto.CreateRuleSetRequest;
import com.attendai.school.attendancerules.dto.CreateSectionOverrideRequest;
import com.attendai.school.attendancerules.dto.UpdateRuleSetRequest;
import com.attendai.school.attendancerules.entity.AttendanceRuleSet;
import com.attendai.school.attendancerules.entity.SectionAttendanceRuleOverride;
import com.attendai.school.attendancerules.exception.AttendanceRuleSetNotFoundException;
import com.attendai.school.attendancerules.mapper.AttendanceRulesMapper;
import com.attendai.school.attendancerules.repository.AttendanceRuleSetRepository;
import com.attendai.school.attendancerules.repository.SectionAttendanceRuleOverrideRepository;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.service.SchoolSectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceRulesServiceImplTest {

    @Mock AttendanceRuleSetRepository             ruleSetRepository;
    @Mock SectionAttendanceRuleOverrideRepository overrideRepository;
    @Mock AttendanceRulesMapper                   rulesMapper;
    @Mock AcademicYearService                     academicYearService;
    @Mock SchoolSectionService                    sectionService;
    @Mock AuditService                            auditService;

    private AttendanceRulesServiceImpl service;

    private static final Long SCHOOL_ID = 1L;
    private static final Long YEAR_ID   = 10L;
    private static final Long SECTION_ID = 20L;

    @BeforeEach
    void setUp() {
        service = new AttendanceRulesServiceImpl(
                ruleSetRepository, overrideRepository, rulesMapper,
                academicYearService, sectionService, auditService);
    }

    // =========================================================================
    // createRuleSet
    // =========================================================================

    @Test
    void createRuleSet_shouldSave_whenValidRequest() {
        stubYear();
        when(ruleSetRepository.existsBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(false);
        AttendanceRuleSet saved = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 15), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.save(any())).thenReturn(saved);
        when(rulesMapper.toResponse(saved)).thenReturn(null);

        service.createRuleSet(SCHOOL_ID, YEAR_ID, buildCreateRequest(
                LocalTime.of(9, 15), new BigDecimal("75.00"), 3));

        verify(ruleSetRepository).save(any(AttendanceRuleSet.class));
        verify(auditService).log(any());
    }

    @Test
    void createRuleSet_shouldThrow409_whenDuplicate() {
        stubYear();
        when(ruleSetRepository.existsBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRuleSet(SCHOOL_ID, YEAR_ID,
                buildCreateRequest(LocalTime.of(9, 0), new BigDecimal("75.00"), 3)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(ruleSetRepository, never()).save(any());
    }

    // =========================================================================
    // getRuleSet
    // =========================================================================

    @Test
    void getRuleSet_shouldThrow404_whenNotFound() {
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRuleSet(SCHOOL_ID, YEAR_ID))
                .isInstanceOf(AttendanceRuleSetNotFoundException.class);
    }

    @Test
    void getRuleSet_shouldReturnResponse_whenFound() {
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(rulesMapper.toResponse(rs)).thenReturn(null);

        service.getRuleSet(SCHOOL_ID, YEAR_ID);

        verify(rulesMapper).toResponse(rs);
    }

    // =========================================================================
    // updateRuleSet
    // =========================================================================

    @Test
    void updateRuleSet_shouldUpdateFields_whenPresent() {
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(ruleSetRepository.save(any())).thenReturn(rs);
        when(rulesMapper.toResponse(any())).thenReturn(null);

        UpdateRuleSetRequest req = new UpdateRuleSetRequest();
        req.setLateThresholdTime(LocalTime.of(9, 15));
        req.setMinAttendancePercentage(new BigDecimal("80.00"));
        service.updateRuleSet(SCHOOL_ID, YEAR_ID, req);

        assertThat(rs.getLateThresholdTime()).isEqualTo(LocalTime.of(9, 15));
        assertThat(rs.getMinAttendancePercentage()).isEqualByComparingTo("80.00");
        assertThat(rs.getConsecutiveAbsenceAlert()).isEqualTo(3); // unchanged
    }

    @Test
    void updateRuleSet_shouldThrow404_whenNotFound() {
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRuleSet(SCHOOL_ID, YEAR_ID,
                new UpdateRuleSetRequest()))
                .isInstanceOf(AttendanceRuleSetNotFoundException.class);
    }

    // =========================================================================
    // createSectionOverride
    // =========================================================================

    @Test
    void createSectionOverride_shouldSave_whenValid() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(overrideRepository.existsByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(false);
        SectionAttendanceRuleOverride saved = buildOverride(1L, 1L, SECTION_ID,
                LocalTime.of(8, 45), null, null);
        when(overrideRepository.save(any())).thenReturn(saved);
        when(rulesMapper.toOverrideResponse(saved)).thenReturn(null);

        CreateSectionOverrideRequest req = new CreateSectionOverrideRequest();
        req.setLateThresholdTime(LocalTime.of(8, 45));
        service.createSectionOverride(SCHOOL_ID, YEAR_ID, SECTION_ID, req);

        verify(overrideRepository).save(any(SectionAttendanceRuleOverride.class));
        verify(auditService).log(any());
    }

    @Test
    void createSectionOverride_shouldThrow_whenSectionBelongsToDifferentSchool() {
        stubSectionInSchool(SECTION_ID, 99L); // school 99, not SCHOOL_ID=1

        assertThatThrownBy(() -> service.createSectionOverride(SCHOOL_ID, YEAR_ID,
                SECTION_ID, new CreateSectionOverrideRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
    }

    @Test
    void createSectionOverride_shouldThrow409_whenDuplicate() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(overrideRepository.existsByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSectionOverride(SCHOOL_ID, YEAR_ID,
                SECTION_ID, new CreateSectionOverrideRequest()))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createSectionOverride_shouldThrow404_whenNoRuleSetExists() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSectionOverride(SCHOOL_ID, YEAR_ID,
                SECTION_ID, new CreateSectionOverrideRequest()))
                .isInstanceOf(AttendanceRuleSetNotFoundException.class);
    }

    // =========================================================================
    // deleteSectionOverride
    // =========================================================================

    @Test
    void deleteSectionOverride_shouldDelete_whenOverrideExists() {
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(overrideRepository.existsByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(true);

        service.deleteSectionOverride(SCHOOL_ID, YEAR_ID, SECTION_ID);

        verify(overrideRepository).deleteByRuleSetIdAndSectionId(1L, SECTION_ID);
        verify(auditService).log(any());
    }

    @Test
    void deleteSectionOverride_shouldThrow_whenNoOverrideExists() {
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(overrideRepository.existsByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.deleteSectionOverride(SCHOOL_ID, YEAR_ID, SECTION_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // getEffectiveRules — merge logic
    // =========================================================================

    @Test
    void getEffectiveRules_shouldReturnSchoolLevel_whenNoOverride() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 15), new BigDecimal("80.00"), 4);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        when(overrideRepository.findByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(Optional.empty());

        var result = service.getEffectiveRules(SCHOOL_ID, YEAR_ID, SECTION_ID);

        assertThat(result.getLateThresholdTime()).isEqualTo(LocalTime.of(9, 15));
        assertThat(result.getMinAttendancePercentage()).isEqualByComparingTo("80.00");
        assertThat(result.getConsecutiveAbsenceAlert()).isEqualTo(4);
        assertThat(result.isFromRuleSet()).isTrue();
    }

    @Test
    void getEffectiveRules_shouldMergeOverride_whenOverrideExists() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));

        // Override only lateThresholdTime — other fields null (use school-level)
        SectionAttendanceRuleOverride ov = buildOverride(1L, 1L, SECTION_ID,
                LocalTime.of(8, 45), null, null);
        when(overrideRepository.findByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(Optional.of(ov));

        var result = service.getEffectiveRules(SCHOOL_ID, YEAR_ID, SECTION_ID);

        // lateThresholdTime overridden
        assertThat(result.getLateThresholdTime()).isEqualTo(LocalTime.of(8, 45));
        // minAttendancePercentage and consecutiveAbsenceAlert from school-level
        assertThat(result.getMinAttendancePercentage()).isEqualByComparingTo("75.00");
        assertThat(result.getConsecutiveAbsenceAlert()).isEqualTo(3);
    }

    @Test
    void getEffectiveRules_shouldReturnDefaults_whenNoRuleSetExists() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        var result = service.getEffectiveRules(SCHOOL_ID, YEAR_ID, SECTION_ID);

        assertThat(result.getLateThresholdTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.getMinAttendancePercentage()).isEqualByComparingTo("75.00");
        assertThat(result.getConsecutiveAbsenceAlert()).isEqualTo(3);
        assertThat(result.isFromRuleSet()).isFalse();
    }

    @Test
    void getEffectiveRules_shouldThrow_whenSectionBelongsToDifferentSchool() {
        stubSectionInSchool(SECTION_ID, 99L);

        assertThatThrownBy(() -> service.getEffectiveRules(SCHOOL_ID, YEAR_ID, SECTION_ID))
                .isInstanceOf(ValidationException.class);
    }

    // =========================================================================
    // Internal API — getLateThreshold
    // =========================================================================

    @Test
    void getLateThreshold_shouldReturnSectionOverrideValue_whenOverrideExists() {
        stubSectionInSchool(SECTION_ID, SCHOOL_ID);
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("75.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));
        SectionAttendanceRuleOverride ov = buildOverride(1L, 1L, SECTION_ID,
                LocalTime.of(8, 30), null, null);
        when(overrideRepository.findByRuleSetIdAndSectionId(1L, SECTION_ID))
                .thenReturn(Optional.of(ov));

        LocalTime threshold = service.getLateThreshold(SECTION_ID, YEAR_ID);

        assertThat(threshold).isEqualTo(LocalTime.of(8, 30));
    }

    @Test
    void getLateThreshold_shouldReturnDefault_whenNoRuleSet() {
        // Internal API findById(sectionId) — resolve schoolId from section
        when(sectionService.findById(SECTION_ID)).thenReturn(
                buildSectionResponse(SECTION_ID, SCHOOL_ID));
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        LocalTime threshold = service.getLateThreshold(SECTION_ID, YEAR_ID);

        assertThat(threshold).isEqualTo(LocalTime.of(9, 0));
    }

    // =========================================================================
    // Internal API — getMinAttendancePercentage
    // =========================================================================

    @Test
    void getMinAttendancePercentage_shouldReturnSchoolLevel_whenRuleSetExists() {
        AttendanceRuleSet rs = buildRuleSet(1L, SCHOOL_ID, YEAR_ID,
                LocalTime.of(9, 0), new BigDecimal("80.00"), 3);
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(rs));

        BigDecimal pct = service.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID);

        assertThat(pct).isEqualByComparingTo("80.00");
    }

    @Test
    void getMinAttendancePercentage_shouldReturnDefault_whenNoRuleSet() {
        when(ruleSetRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.empty());

        BigDecimal pct = service.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID);

        assertThat(pct).isEqualByComparingTo("75.00");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubYear() {
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(
                AcademicYearResponse.builder()
                        .id(YEAR_ID).schoolId(SCHOOL_ID).name("2025-2026")
                        .startDate(LocalDate.of(2025, 6, 1))
                        .endDate(LocalDate.of(2026, 3, 31))
                        .status(AcademicYearStatus.ACTIVE)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                        .build());
    }

    private void stubSectionInSchool(Long sectionId, Long schoolId) {
        when(sectionService.findById(sectionId))
                .thenReturn(buildSectionResponse(sectionId, schoolId));
    }

    private SectionResponse buildSectionResponse(Long id, Long schoolId) {
        return SectionResponse.builder()
                .id(id).schoolId(schoolId).classId(5L).academicYearId(YEAR_ID)
                .name("A").status(SectionStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private AttendanceRuleSet buildRuleSet(Long id, Long schoolId, Long yearId,
                                            LocalTime late, BigDecimal pct, int alert) {
        AttendanceRuleSet rs = AttendanceRuleSet.builder()
                .schoolId(schoolId).academicYearId(yearId)
                .lateThresholdTime(late)
                .minAttendancePercentage(pct)
                .consecutiveAbsenceAlert(alert).build();
        rs.setId(id);
        return rs;
    }

    private SectionAttendanceRuleOverride buildOverride(Long id, Long ruleSetId,
                                                         Long sectionId, LocalTime late,
                                                         BigDecimal pct, Integer alert) {
        SectionAttendanceRuleOverride ov = SectionAttendanceRuleOverride.builder()
                .ruleSetId(ruleSetId).sectionId(sectionId)
                .lateThresholdTime(late)
                .minAttendancePercentage(pct)
                .consecutiveAbsenceAlert(alert).build();
        ov.setId(id);
        return ov;
    }

    private CreateRuleSetRequest buildCreateRequest(LocalTime late, BigDecimal pct, int alert) {
        CreateRuleSetRequest req = new CreateRuleSetRequest();
        req.setLateThresholdTime(late);
        req.setMinAttendancePercentage(pct);
        req.setConsecutiveAbsenceAlert(alert);
        return req;
    }
}
