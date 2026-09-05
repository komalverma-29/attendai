package com.attendai.school.dashboard.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.dashboard.dto.AttendanceTrendResponse;
import com.attendai.school.dashboard.dto.LowAttendanceAlertResponse;
import com.attendai.school.dashboard.dto.SchoolOverviewResponse;
import com.attendai.school.dashboard.dto.SectionDailySummaryResponse;
import com.attendai.school.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** FR-DASH-01: School-wide overview for today. */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('SCHOOL_DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<SchoolOverviewResponse>> getOverview(
            @PathVariable Long schoolId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getSchoolOverview(schoolId)));
    }

    /** FR-DASH-02: Per-section attendance summary for today. */
    @GetMapping("/sections/summary")
    @PreAuthorize("hasAuthority('SCHOOL_DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<List<SectionDailySummaryResponse>>> getSectionsSummary(
            @PathVariable Long schoolId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getSectionsSummaryToday(schoolId)));
    }

    /** FR-DASH-03: Attendance trend for last 30 working days. */
    @GetMapping("/trend")
    @PreAuthorize("hasAuthority('SCHOOL_DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<AttendanceTrendResponse>> getTrend(
            @PathVariable Long schoolId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getAttendanceTrend(schoolId, sectionId)));
    }

    /** FR-DASH-04: Students below minimum attendance threshold. */
    @GetMapping("/low-attendance")
    @PreAuthorize("hasAuthority('SCHOOL_DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<List<LowAttendanceAlertResponse>>> getLowAttendance(
            @PathVariable Long schoolId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getLowAttendanceAlerts(schoolId, sectionId)));
    }

    /**
     * FR-DASH-06: Section dashboard — accessible by school admin (SCHOOL_DASHBOARD_READ)
     * or teacher with (SCHOOL_TEACHER_DASHBOARD_READ).
     */
    @GetMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_DASHBOARD_READ','SCHOOL_TEACHER_DASHBOARD_READ')")
    public ResponseEntity<ApiResponse<SectionDailySummaryResponse>> getSectionDashboard(
            @PathVariable Long schoolId,
            @PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getSectionDashboard(schoolId, sectionId)));
    }
}
