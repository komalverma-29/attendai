package com.attendai.school.leave.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.leave.dto.CreateLeaveApplicationRequest;
import com.attendai.school.leave.dto.LeaveApplicationResponse;
import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import com.attendai.school.leave.exception.LeaveApplicationNotFoundException;
import com.attendai.school.leave.service.LeaveApplicationService;
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

@WebMvcTest(LeaveApplicationController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class LeaveApplicationControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean LeaveApplicationService leaveService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/leave";

    // =========================================================================
    // POST — create leave
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_REQUEST")
    void createLeave_shouldReturn201_whenValid() throws Exception {
        when(leaveService.createLeave(anyLong(), any())).thenReturn(buildResponse());

        CreateLeaveApplicationRequest req = buildRequest();
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_REQUEST")
    void createLeave_shouldReturn400_whenApplicantTypeMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"leaveType\":\"SICK\",\"startDate\":\"2025-10-10\","
                                + "\"endDate\":\"2025-10-12\",\"reason\":\"Sick\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_REQUEST")
    void createLeave_shouldReturn409_whenOverlapping() throws Exception {
        when(leaveService.createLeave(anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Overlapping leave"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_REQUEST")
    void createLeave_shouldReturn400_whenEndBeforeStart() throws Exception {
        when(leaveService.createLeave(anyLong(), any()))
                .thenThrow(new ValidationException("End date must be on or after start date"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLeave_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_READ")
    void createLeave_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_READ")
    void getLeave_shouldReturn200_whenFound() throws Exception {
        when(leaveService.findById(1L, 1L)).thenReturn(buildResponse());
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_READ")
    void getLeave_shouldReturn404_whenNotFound() throws Exception {
        when(leaveService.findById(1L, 99L))
                .thenThrow(new LeaveApplicationNotFoundException(99L));
        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getLeave_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // PATCH /{id}/approve
    // =========================================================================

    @Test
    @WithMockUser(username = "5", authorities = "SCHOOL_LEAVE_MANAGE")
    void approveLeave_shouldReturn200_whenValid() throws Exception {
        when(leaveService.approveLeave(anyLong(), anyLong(), any(), any()))
                .thenReturn(buildResponse(LeaveStatus.APPROVED));
        mockMvc.perform(patch(BASE + "/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_MANAGE")
    void approveLeave_shouldReturn400_whenNotPending() throws Exception {
        when(leaveService.approveLeave(anyLong(), anyLong(), any(), any()))
                .thenThrow(new ValidationException("Cannot approve a leave with status: REJECTED"));
        mockMvc.perform(patch(BASE + "/1/approve"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveLeave_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/1/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_LEAVE_READ")
    void approveLeave_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(patch(BASE + "/1/approve")).andExpect(status().isForbidden());
    }

    // =========================================================================
    // PATCH /{id}/reject
    // =========================================================================

    @Test
    @WithMockUser(username = "5", authorities = "SCHOOL_LEAVE_MANAGE")
    void rejectLeave_shouldReturn200_whenValid() throws Exception {
        when(leaveService.rejectLeave(anyLong(), anyLong(), any(), any()))
                .thenReturn(buildResponse(LeaveStatus.REJECTED));
        mockMvc.perform(patch(BASE + "/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    // =========================================================================
    // PATCH /{id}/cancel
    // =========================================================================

    @Test
    @WithMockUser(username = "30")
    void cancelLeave_shouldReturn200_whenAuthenticated() throws Exception {
        when(leaveService.cancelLeave(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(LeaveStatus.CANCELLED));
        mockMvc.perform(patch(BASE + "/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void cancelLeave_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch(BASE + "/1/cancel")).andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // PATCH /{id}/revoke
    // =========================================================================

    @Test
    @WithMockUser(username = "5", authorities = "SCHOOL_LEAVE_MANAGE")
    void revokeLeave_shouldReturn200_whenValid() throws Exception {
        when(leaveService.revokeLeave(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(LeaveStatus.REVOKED));
        mockMvc.perform(patch(BASE + "/1/revoke"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LeaveApplicationResponse buildResponse() {
        return buildResponse(LeaveStatus.PENDING);
    }

    private LeaveApplicationResponse buildResponse(LeaveStatus status) {
        return LeaveApplicationResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L)
                .applicantType(LeaveApplicantType.STUDENT).studentId(30L)
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(2))
                .totalDays(3).status(status).reason("Sick")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private CreateLeaveApplicationRequest buildRequest() {
        CreateLeaveApplicationRequest req = new CreateLeaveApplicationRequest();
        req.setApplicantType(LeaveApplicantType.STUDENT);
        req.setStudentId(30L);
        req.setLeaveType(LeaveType.SICK);
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusDays(2));
        req.setReason("Sick");
        return req;
    }
}
