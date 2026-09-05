package com.attendai.school.leave.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.leave.dto.CreateLeaveApplicationRequest;
import com.attendai.school.leave.dto.LeaveApplicationResponse;
import com.attendai.school.leave.dto.LeaveApplicationSummaryResponse;
import com.attendai.school.leave.dto.ReviewLeaveRequest;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import com.attendai.school.leave.service.LeaveApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/leave")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_LEAVE_REQUEST','SCHOOL_LEAVE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> createLeave(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateLeaveApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(leaveService.createLeave(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_LEAVE_READ')")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getLeave(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_LEAVE_READ')")
    public ResponseEntity<PageResponse<LeaveApplicationSummaryResponse>> listLeaves(
            @PathVariable("schoolId")                          Long        schoolId,
            @RequestParam(value = "studentId",  required = false) Long        studentId,
            @RequestParam(value = "teacherId",  required = false) Long        teacherId,
            @RequestParam(value = "status",     required = false) LeaveStatus status,
            @RequestParam(value = "leaveType",  required = false) LeaveType   leaveType,
            @RequestParam(value = "fromDate",   required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate",     required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                leaveService.listLeaves(schoolId, studentId, teacherId, status, leaveType,
                        fromDate, toDate, pageParams.toPageable())));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('SCHOOL_LEAVE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> approveLeave(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @RequestBody(required = false) ReviewLeaveRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long actorUserId = resolveUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.approveLeave(schoolId, id, request, actorUserId)));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SCHOOL_LEAVE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> rejectLeave(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @RequestBody(required = false) ReviewLeaveRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long actorUserId = resolveUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.rejectLeave(schoolId, id, request, actorUserId)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> cancelLeave(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long actorUserId = resolveUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.cancelLeave(schoolId, id, actorUserId)));
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('SCHOOL_LEAVE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> revokeLeave(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long actorUserId = resolveUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                leaveService.revokeLeave(schoolId, id, actorUserId)));
    }

    private Long resolveUserId(
            org.springframework.security.core.userdetails.UserDetails principal) {
        if (principal == null) return null;
        try { return Long.parseLong(principal.getUsername()); } catch (Exception e) { return null; }
    }
}
