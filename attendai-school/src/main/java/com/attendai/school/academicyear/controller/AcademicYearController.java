package com.attendai.school.academicyear.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.dto.AcademicYearSummaryResponse;
import com.attendai.school.academicyear.dto.CreateAcademicYearRequest;
import com.attendai.school.academicyear.dto.UpdateAcademicYearRequest;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_CREATE')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> createAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateAcademicYearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        academicYearService.createAcademicYear(schoolId, request)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_READ')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getActiveAcademicYear(
            @PathVariable("schoolId") Long schoolId) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.getActiveAcademicYearOrThrow(schoolId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_READ')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_READ')")
    public ResponseEntity<PageResponse<AcademicYearSummaryResponse>> listAcademicYears(
            @PathVariable("schoolId")                      Long               schoolId,
            @RequestParam(name = "status", required = false) AcademicYearStatus status,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                academicYearService.listAcademicYears(schoolId, status, pageParams.toPageable())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_UPDATE')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> updateAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.updateAcademicYear(schoolId, id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_UPDATE')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> activateAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.activateAcademicYear(schoolId, id)));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_UPDATE')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> completeAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.completeAcademicYear(schoolId, id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_UPDATE')")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> cancelAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                academicYearService.cancelAcademicYear(schoolId, id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ACADEMIC_YEAR_DELETE')")
    public ResponseEntity<Void> deleteAcademicYear(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        academicYearService.deleteAcademicYear(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
