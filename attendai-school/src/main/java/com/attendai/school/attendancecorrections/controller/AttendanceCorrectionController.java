package com.attendai.school.attendancecorrections.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.attendancecorrections.dto.CorrectionRequestResponse;
import com.attendai.school.attendancecorrections.dto.CorrectionSummaryResponse;
import com.attendai.school.attendancecorrections.dto.CreateCorrectionRequest;
import com.attendai.school.attendancecorrections.dto.ReviewCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.service.AttendanceCorrectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/attendance/corrections")
@RequiredArgsConstructor
public class AttendanceCorrectionController {

    private final AttendanceCorrectionService correctionService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_CORRECTION_REQUEST')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> submit(
            @PathVariable Long schoolId,
            @Valid @RequestBody CreateCorrectionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        correctionService.submitCorrection(schoolId, request, userId(principal))));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_CORRECTION_READ')")
    public ResponseEntity<PageResponse<CorrectionSummaryResponse>> list(
            @PathVariable Long schoolId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) CorrectionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                correctionService.listCorrections(schoolId, studentId, status,
                        fromDate, toDate, pageParams.toPageable())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_CORRECTION_READ')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> getById(
            @PathVariable Long schoolId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(correctionService.findById(schoolId, id)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_CORRECTION_APPROVE')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> approve(
            @PathVariable Long schoolId, @PathVariable Long id,
            @RequestBody(required = false) ReviewCorrectionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                correctionService.approveCorrection(schoolId, id, request, userId(principal))));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_CORRECTION_APPROVE')")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> reject(
            @PathVariable Long schoolId, @PathVariable Long id,
            @RequestBody(required = false) ReviewCorrectionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                correctionService.rejectCorrection(schoolId, id, request, userId(principal))));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CorrectionRequestResponse>> cancel(
            @PathVariable Long schoolId, @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success(
                correctionService.cancelCorrection(schoolId, id, userId(principal))));
    }

    private Long userId(UserDetails p) {
        if (p == null) return null;
        try { return Long.parseLong(p.getUsername()); } catch (Exception e) { return null; }
    }
}
