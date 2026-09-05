package com.attendai.school.dailyattendance.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.dailyattendance.dto.DailyAttendanceRecordResponse;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.dto.SectionAttendanceSummaryResponse;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/attendance")
@RequiredArgsConstructor
public class DailyAttendanceController {

    private final DailyAttendanceService attendanceService;

    @GetMapping("/sections/{sectionId}/daily")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<SectionAttendanceSummaryResponse>> getSectionAttendance(
            @PathVariable("schoolId")  Long      schoolId,
            @PathVariable("sectionId") Long      sectionId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getSectionAttendanceForDate(schoolId, sectionId, date)));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<List<DailyAttendanceRecordResponse>>> getStudentAttendance(
            @PathVariable("schoolId")  Long      schoolId,
            @PathVariable("studentId") Long      studentId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "academicYearId", required = false) Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getStudentAttendance(schoolId, studentId, fromDate, toDate, academicYearId)));
    }

    @PatchMapping("/records/{id}/override")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_OVERRIDE')")
    public ResponseEntity<ApiResponse<DailyAttendanceRecordResponse>> overrideAttendance(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long recordId,
            @Valid @RequestBody OverrideAttendanceRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        Long actorUserId = null;
        if (principal != null) {
            try { actorUserId = Long.parseLong(principal.getUsername()); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.overrideAttendance(schoolId, recordId, request, actorUserId)));
    }
}
