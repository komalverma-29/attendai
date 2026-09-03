package com.attendai.school.attendancerules.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.attendancerules.dto.AttendanceRuleSetResponse;
import com.attendai.school.attendancerules.dto.CreateRuleSetRequest;
import com.attendai.school.attendancerules.dto.CreateSectionOverrideRequest;
import com.attendai.school.attendancerules.dto.EffectiveRulesResponse;
import com.attendai.school.attendancerules.dto.SectionOverrideResponse;
import com.attendai.school.attendancerules.dto.UpdateRuleSetRequest;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/attendance-rules")
@RequiredArgsConstructor
public class AttendanceRulesController {

    private final AttendanceRulesService rulesService;

    // -------------------------------------------------------------------------
    // Rule set CRUD
    // -------------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceRuleSetResponse>> createRuleSet(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody CreateRuleSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        rulesService.createRuleSet(schoolId, academicYearId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_READ')")
    public ResponseEntity<ApiResponse<AttendanceRuleSetResponse>> getRuleSet(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                rulesService.getRuleSet(schoolId, academicYearId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceRuleSetResponse>> updateRuleSet(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody UpdateRuleSetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rulesService.updateRuleSet(schoolId, academicYearId, request)));
    }

    // -------------------------------------------------------------------------
    // Section override
    // -------------------------------------------------------------------------

    @PostMapping("/sections/{sectionId}/override")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_MANAGE')")
    public ResponseEntity<ApiResponse<SectionOverrideResponse>> createSectionOverride(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("sectionId")      Long sectionId,
            @Valid @RequestBody CreateSectionOverrideRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        rulesService.createSectionOverride(
                                schoolId, academicYearId, sectionId, request)));
    }

    @DeleteMapping("/sections/{sectionId}/override")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_MANAGE')")
    public ResponseEntity<Void> deleteSectionOverride(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("sectionId")      Long sectionId) {
        rulesService.deleteSectionOverride(schoolId, academicYearId, sectionId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Effective rules
    // -------------------------------------------------------------------------

    @GetMapping("/sections/{sectionId}/effective")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_RULES_READ')")
    public ResponseEntity<ApiResponse<EffectiveRulesResponse>> getEffectiveRules(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("sectionId")      Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                rulesService.getEffectiveRules(schoolId, academicYearId, sectionId)));
    }
}
