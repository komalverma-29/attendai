package com.attendai.school.attendancecorrections.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.attendancecorrections.dto.CorrectionRequestResponse;
import com.attendai.school.attendancecorrections.dto.CreateCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.exception.CorrectionRequestNotFoundException;
import com.attendai.school.attendancecorrections.service.AttendanceCorrectionService;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AttendanceCorrectionController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AttendanceCorrectionControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AttendanceCorrectionService correctionService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/attendance/corrections";

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_REQUEST")
    void submit_shouldReturn201_whenValid() throws Exception {
        when(correctionService.submitCorrection(anyLong(), any(), any())).thenReturn(buildResponse());

        CreateCorrectionRequest req = new CreateCorrectionRequest();
        req.setStudentId(30L);
        req.setAttendanceDate(LocalDate.now().minusDays(1));
        req.setRequestedStatus(DailyAttendanceStatus.PRESENT);
        req.setReason("Station offline");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_REQUEST")
    void submit_shouldReturn400_whenStudentIdMissing() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendanceDate\":\"2025-10-08\",\"requestedStatus\":\"PRESENT\",\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_REQUEST")
    void submit_shouldReturn409_whenDuplicatePending() throws Exception {
        when(correctionService.submitCorrection(anyLong(), any(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Duplicate pending"));

        CreateCorrectionRequest req = new CreateCorrectionRequest();
        req.setStudentId(30L); req.setAttendanceDate(LocalDate.now().minusDays(1));
        req.setRequestedStatus(DailyAttendanceStatus.PRESENT); req.setReason("x");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_REQUEST")
    void submit_shouldReturn400_whenOnLeave() throws Exception {
        when(correctionService.submitCorrection(anyLong(), any(), any()))
                .thenThrow(new ValidationException("Use school-leave to set ON_LEAVE"));

        CreateCorrectionRequest req = new CreateCorrectionRequest();
        req.setStudentId(30L); req.setAttendanceDate(LocalDate.now().minusDays(1));
        req.setRequestedStatus(DailyAttendanceStatus.ON_LEAVE); req.setReason("x");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submit_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_READ")
    void getById_shouldReturn200_whenFound() throws Exception {
        when(correctionService.findById(1L, 1L)).thenReturn(buildResponse());
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_READ")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(correctionService.findById(1L, 99L))
                .thenThrow(new CorrectionRequestNotFoundException(99L));
        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "99", authorities = "SCHOOL_ATTENDANCE_CORRECTION_APPROVE")
    void approve_shouldReturn200_whenValid() throws Exception {
        when(correctionService.approveCorrection(anyLong(), anyLong(), any(), any()))
                .thenReturn(buildResponse(CorrectionStatus.APPROVED));
        mockMvc.perform(patch(BASE + "/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "99", authorities = "SCHOOL_ATTENDANCE_CORRECTION_APPROVE")
    void reject_shouldReturn200_whenValid() throws Exception {
        when(correctionService.rejectCorrection(anyLong(), anyLong(), any(), any()))
                .thenReturn(buildResponse(CorrectionStatus.REJECTED));
        mockMvc.perform(patch(BASE + "/1/reject"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "5")
    void cancel_shouldReturn200_whenAuthenticated() throws Exception {
        when(correctionService.cancelCorrection(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(CorrectionStatus.CANCELLED));
        mockMvc.perform(patch(BASE + "/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/1/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_CORRECTION_READ")
    void list_shouldReturn200() throws Exception {
        when(correctionService.listCorrections(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mockMvc.perform(get(BASE)).andExpect(status().isOk());
    }

    // =========================================================================
    private CorrectionRequestResponse buildResponse() { return buildResponse(CorrectionStatus.PENDING); }
    private CorrectionRequestResponse buildResponse(CorrectionStatus status) {
        return CorrectionRequestResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L).studentId(30L).attendanceRecordId(100L)
                .attendanceDate(LocalDate.now().minusDays(1))
                .originalStatus(DailyAttendanceStatus.ABSENT)
                .requestedStatus(DailyAttendanceStatus.PRESENT)
                .reason("Station offline").status(status).requestedById(5L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
