package com.attendai.school.attendancereports.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.attendancereports.dto.SchoolAttendanceOverviewResponse;
import com.attendai.school.attendancereports.dto.StudentAttendanceSummaryResponse;
import com.attendai.school.attendancereports.service.AttendanceReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceReportController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AttendanceReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AttendanceReportService reportService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/reports/attendance";

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void studentSummary_shouldReturn200() throws Exception {
        StudentAttendanceSummaryResponse resp = StudentAttendanceSummaryResponse.builder()
                .studentId(30L).academicYearId(10L)
                .fromDate(LocalDate.of(2025, 6, 1)).toDate(LocalDate.of(2025, 10, 31))
                .workingDays(90).presentDays(72).lateDays(5).absentDays(8).onLeaveDays(5)
                .attendancePercentage(new BigDecimal("85.56"))
                .minimumRequired(new BigDecimal("75.00")).belowThreshold(false).build();
        when(reportService.getStudentSummary(anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(resp);

        mockMvc.perform(get(BASE + "/students/30/summary?academicYearId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(30))
                .andExpect(jsonPath("$.data.belowThreshold").value(false));
    }

    @Test
    void studentSummary_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/students/30/summary?academicYearId=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void studentSummary_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/students/30/summary?academicYearId=10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void studentSummary_shouldReturn400_whenAcademicYearIdMissing() throws Exception {
        mockMvc.perform(get(BASE + "/students/30/summary"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void sectionSummary_shouldReturn200() throws Exception {
        when(reportService.getSectionSummary(anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(List.of());
        mockMvc.perform(get(BASE + "/sections/20/summary?academicYearId=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void shortage_shouldReturn200() throws Exception {
        when(reportService.getShortageReport(anyLong(), anyLong(), any()))
                .thenReturn(List.of());
        mockMvc.perform(get(BASE + "/shortage?academicYearId=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void register_shouldReturn200() throws Exception {
        when(reportService.getDailyRegister(anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(com.attendai.school.attendancereports.dto.DailyAttendanceRegisterResponse
                        .builder().sectionId(20L).workingDates(List.of()).students(List.of()).build());
        mockMvc.perform(get(BASE + "/sections/20/register?academicYearId=10&fromDate=2025-06-01&toDate=2025-10-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void consecutiveAbsences_shouldReturn200() throws Exception {
        when(reportService.getConsecutiveAbsences(anyLong(), anyLong(), any(), anyInt()))
                .thenReturn(List.of());
        mockMvc.perform(get(BASE + "/consecutive-absences?academicYearId=10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_REPORT_READ")
    void overview_shouldReturn200() throws Exception {
        when(reportService.getSchoolOverview(anyLong(), any(), any()))
                .thenReturn(SchoolAttendanceOverviewResponse.builder()
                        .schoolId(1L).fromDate(LocalDate.of(2025,9,1)).toDate(LocalDate.of(2025,9,30))
                        .totalStudents(50).presentCount(40).lateCount(5).absentCount(3).onLeaveCount(2)
                        .attendancePercentage(new BigDecimal("90.00")).build());
        mockMvc.perform(get(BASE + "/overview?fromDate=2025-09-01&toDate=2025-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(50));
    }
}
