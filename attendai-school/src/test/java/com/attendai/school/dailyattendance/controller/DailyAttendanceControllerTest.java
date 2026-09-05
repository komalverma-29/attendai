package com.attendai.school.dailyattendance.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.dailyattendance.dto.DailyAttendanceRecordResponse;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.dto.SectionAttendanceSummaryResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.exception.AttendanceRecordNotFoundException;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DailyAttendanceController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class DailyAttendanceControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DailyAttendanceService attendanceService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/attendance";

    // =========================================================================
    // GET /sections/{sectionId}/daily
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void getSectionAttendance_shouldReturn200_whenValid() throws Exception {
        SectionAttendanceSummaryResponse resp = SectionAttendanceSummaryResponse.builder()
                .sectionId(20L).date(LocalDate.of(2025, 10, 8))
                .workingDay(true).records(List.of())
                .present(5).late(1).absent(2).onLeave(0).build();
        when(attendanceService.getSectionAttendanceForDate(1L, 20L, LocalDate.of(2025, 10, 8)))
                .thenReturn(resp);

        mockMvc.perform(get(BASE + "/sections/20/daily?date=2025-10-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(20))
                .andExpect(jsonPath("$.data.workingDay").value(true))
                .andExpect(jsonPath("$.data.present").value(5));
    }

    @Test
    void getSectionAttendance_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20/daily?date=2025-10-08"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_OVERRIDE")
    void getSectionAttendance_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20/daily?date=2025-10-08"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void getSectionAttendance_shouldReturn400_whenDateMissing() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20/daily"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void getSectionAttendance_shouldReturn400_whenCrossSchool() throws Exception {
        when(attendanceService.getSectionAttendanceForDate(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Section does not belong to school"));

        mockMvc.perform(get(BASE + "/sections/20/daily?date=2025-10-08"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // =========================================================================
    // GET /students/{studentId}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void getStudentAttendance_shouldReturn200_whenValid() throws Exception {
        when(attendanceService.getStudentAttendance(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of(buildRecordResponse()));

        mockMvc.perform(get(BASE + "/students/30?fromDate=2025-10-01&toDate=2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].studentId").value(30));
    }

    @Test
    void getStudentAttendance_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/students/30?fromDate=2025-10-01&toDate=2025-10-31"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // PATCH /records/{id}/override
    // =========================================================================

    @Test
    @WithMockUser(username = "5", authorities = "SCHOOL_ATTENDANCE_OVERRIDE")
    void overrideAttendance_shouldReturn200_whenValid() throws Exception {
        when(attendanceService.overrideAttendance(anyLong(), anyLong(), any(), any()))
                .thenReturn(buildRecordResponse());

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);
        req.setRemarks("Station malfunction");

        mockMvc.perform(patch(BASE + "/records/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABSENT")); // from buildRecordResponse
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_OVERRIDE")
    void overrideAttendance_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/records/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_OVERRIDE")
    void overrideAttendance_shouldReturn404_whenNotFound() throws Exception {
        when(attendanceService.overrideAttendance(anyLong(), anyLong(), any(), any()))
                .thenThrow(new AttendanceRecordNotFoundException(99L));

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);

        mockMvc.perform(patch(BASE + "/records/99/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void overrideAttendance_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/records/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_READ")
    void overrideAttendance_shouldReturn403_whenWrongPermission() throws Exception {
        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);
        mockMvc.perform(patch(BASE + "/records/1/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private DailyAttendanceRecordResponse buildRecordResponse() {
        return DailyAttendanceRecordResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L).sectionId(20L).studentId(30L)
                .attendanceDate(LocalDate.of(2025, 10, 8))
                .status(DailyAttendanceStatus.ABSENT)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
