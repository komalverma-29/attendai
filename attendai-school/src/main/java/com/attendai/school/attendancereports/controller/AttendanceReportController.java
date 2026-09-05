package com.attendai.school.attendancereports.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.attendancereports.dto.AttendanceShortageResponse;
import com.attendai.school.attendancereports.dto.ConsecutiveAbsenceResponse;
import com.attendai.school.attendancereports.dto.DailyAttendanceRegisterResponse;
import com.attendai.school.attendancereports.dto.SchoolAttendanceOverviewResponse;
import com.attendai.school.attendancereports.dto.StudentAttendanceSummaryResponse;
import com.attendai.school.attendancereports.service.AttendanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/reports/attendance")
@RequiredArgsConstructor
public class AttendanceReportController {

    private final AttendanceReportService reportService;

    @GetMapping("/students/{studentId}/summary")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> studentSummary(
            @PathVariable Long schoolId,
            @PathVariable Long studentId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getStudentSummary(schoolId, studentId, academicYearId, fromDate, toDate)));
    }

    @GetMapping("/sections/{sectionId}/summary")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<List<StudentAttendanceSummaryResponse>>> sectionSummary(
            @PathVariable Long schoolId,
            @PathVariable Long sectionId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getSectionSummary(schoolId, sectionId, academicYearId, fromDate, toDate)));
    }

    @GetMapping("/shortage")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<List<AttendanceShortageResponse>>> shortage(
            @PathVariable Long schoolId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getShortageReport(schoolId, academicYearId, sectionId)));
    }

    @GetMapping("/sections/{sectionId}/register")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<DailyAttendanceRegisterResponse>> register(
            @PathVariable Long schoolId,
            @PathVariable Long sectionId,
            @RequestParam Long academicYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getDailyRegister(schoolId, sectionId, academicYearId, fromDate, toDate)));
    }

    @GetMapping("/consecutive-absences")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<List<ConsecutiveAbsenceResponse>>> consecutiveAbsences(
            @PathVariable Long schoolId,
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(defaultValue = "3") int minConsecutiveDays) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getConsecutiveAbsences(schoolId, academicYearId, sectionId, minConsecutiveDays)));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('SCHOOL_ATTENDANCE_REPORT_READ')")
    public ResponseEntity<ApiResponse<SchoolAttendanceOverviewResponse>> overview(
            @PathVariable Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getSchoolOverview(schoolId, fromDate, toDate)));
    }
}
