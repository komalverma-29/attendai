package com.attendai.school.dashboard.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.dashboard.dto.AttendanceTrendResponse;
import com.attendai.school.dashboard.dto.PendingActionsResponse;
import com.attendai.school.dashboard.dto.SchoolOverviewResponse;
import com.attendai.school.dashboard.dto.SectionDailySummaryResponse;
import com.attendai.school.dashboard.service.DashboardService;
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

@WebMvcTest(DashboardController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean DashboardService dashboardService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/dashboard";

    // =========================================================================
    // GET /overview
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void overview_shouldReturn200_whenWorkingDay() throws Exception {
        when(dashboardService.getSchoolOverview(1L)).thenReturn(
                SchoolOverviewResponse.builder()
                        .date(LocalDate.now()).workingDay(true).academicYearId(10L)
                        .totalStudents(100).present(80).late(5).absent(10).onLeave(5)
                        .attendancePercentage(new BigDecimal("85.00"))
                        .pendingActions(PendingActionsResponse.builder()
                                .correctionRequests(2).leaveApplications(3).consecutiveAbsenceAlerts(1).build())
                        .build());

        mockMvc.perform(get(BASE + "/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workingDay").value(true))
                .andExpect(jsonPath("$.data.totalStudents").value(100))
                .andExpect(jsonPath("$.data.pendingActions.correctionRequests").value(2));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void overview_shouldReturn200_whenNotWorkingDay() throws Exception {
        when(dashboardService.getSchoolOverview(1L)).thenReturn(
                SchoolOverviewResponse.builder()
                        .date(LocalDate.now()).workingDay(false).academicYearId(null)
                        .totalStudents(0).present(0).late(0).absent(0).onLeave(0)
                        .attendancePercentage(BigDecimal.ZERO)
                        .pendingActions(PendingActionsResponse.builder()
                                .correctionRequests(0).leaveApplications(0).consecutiveAbsenceAlerts(0).build())
                        .build());

        mockMvc.perform(get(BASE + "/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workingDay").value(false))
                .andExpect(jsonPath("$.data.totalStudents").value(0));
    }

    @Test
    void overview_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void overview_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/overview")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /sections/summary
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void sectionsSummary_shouldReturn200() throws Exception {
        when(dashboardService.getSectionsSummaryToday(1L)).thenReturn(
                List.of(SectionDailySummaryResponse.builder()
                        .sectionId(20L).sectionName("A").totalStudents(40)
                        .present(35).late(2).absent(2).onLeave(1)
                        .attendancePercentage(new BigDecimal("92.50")).build()));

        mockMvc.perform(get(BASE + "/sections/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sectionId").value(20))
                .andExpect(jsonPath("$.data[0].present").value(35));
    }

    // =========================================================================
    // GET /trend
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void trend_shouldReturn200() throws Exception {
        when(dashboardService.getAttendanceTrend(1L, null)).thenReturn(
                AttendanceTrendResponse.builder().sectionId(null).trend(List.of(
                        AttendanceTrendResponse.TrendPoint.builder()
                                .date(LocalDate.now().minusDays(1))
                                .percentage(new BigDecimal("88.50")).build()
                )).build());

        mockMvc.perform(get(BASE + "/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trend[0].percentage").value(88.50));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void trend_shouldReturn200_withSectionFilter() throws Exception {
        when(dashboardService.getAttendanceTrend(1L, 20L)).thenReturn(
                AttendanceTrendResponse.builder().sectionId(20L).trend(List.of()).build());

        mockMvc.perform(get(BASE + "/trend?sectionId=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(20));
    }

    // =========================================================================
    // GET /low-attendance
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void lowAttendance_shouldReturn200() throws Exception {
        when(dashboardService.getLowAttendanceAlerts(1L, null)).thenReturn(List.of());
        mockMvc.perform(get(BASE + "/low-attendance"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GET /sections/{sectionId}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_DASHBOARD_READ")
    void sectionDashboard_shouldReturn200_withAdminPermission() throws Exception {
        when(dashboardService.getSectionDashboard(1L, 20L)).thenReturn(
                SectionDailySummaryResponse.builder()
                        .sectionId(20L).totalStudents(35)
                        .present(30).late(2).absent(2).onLeave(1)
                        .attendancePercentage(new BigDecimal("91.18")).build());

        mockMvc.perform(get(BASE + "/sections/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(20));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_DASHBOARD_READ")
    void sectionDashboard_shouldReturn200_withTeacherPermission() throws Exception {
        when(dashboardService.getSectionDashboard(1L, 20L)).thenReturn(
                SectionDailySummaryResponse.builder()
                        .sectionId(20L).totalStudents(35).present(30).late(2).absent(2).onLeave(1)
                        .attendancePercentage(new BigDecimal("91.18")).build());

        mockMvc.perform(get(BASE + "/sections/20"))
                .andExpect(status().isOk());
    }

    @Test
    void sectionDashboard_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void sectionDashboard_shouldReturn403_whenNoPermission() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20")).andExpect(status().isForbidden());
    }
}
