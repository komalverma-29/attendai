package com.attendai.core.attendance.controller;

import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.dto.MarkProcessedRequest;
import com.attendai.core.attendance.dto.RecordManualEventRequest;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import com.attendai.core.attendance.exception.AttendanceEventNotFoundException;
import com.attendai.core.attendance.service.AttendanceService;
import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.station.config.StationSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceController.class)
@Import({SecurityConfig.class, StationSecurityConfig.class})
class AttendanceControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AttendanceService attendanceService;

    // Required by SecurityConfig and StationSecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService         jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties   securityProperties;
    @MockBean com.attendai.core.station.service.StationService   stationService;

    private static final String BASE = "/api/v1/core/attendance";

    // -------------------------------------------------------------------------
    // POST /events/manual
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_RECORD_MANUAL")
    void recordManualEvent_shouldReturn201_whenValid() throws Exception {
        AttendanceEventResponse resp = buildResponse(AttendanceEventStatus.PENDING);
        when(attendanceService.recordManualEvent(any())).thenReturn(resp);

        RecordManualEventRequest req = new RecordManualEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().minusMinutes(10));
        req.setDirection(EventDirection.ENTRY);

        mockMvc.perform(post(BASE + "/events/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_RECORD_MANUAL")
    void recordManualEvent_shouldReturn400_whenPersonIdMissing() throws Exception {
        RecordManualEventRequest req = new RecordManualEventRequest();
        req.setEventTime(LocalDateTime.now().minusMinutes(5));
        req.setDirection(EventDirection.ENTRY);
        // personId missing

        mockMvc.perform(post(BASE + "/events/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void recordManualEvent_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE + "/events/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_READ")
    void recordManualEvent_shouldReturn403_whenWrongPermission() throws Exception {
        RecordManualEventRequest req = new RecordManualEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().minusMinutes(5));
        req.setDirection(EventDirection.ENTRY);

        mockMvc.perform(post(BASE + "/events/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /events/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_READ")
    void getEvent_shouldReturn200_whenFound() throws Exception {
        when(attendanceService.findById(1L)).thenReturn(buildResponse(AttendanceEventStatus.PENDING));

        mockMvc.perform(get(BASE + "/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_READ")
    void getEvent_shouldReturn404_whenNotFound() throws Exception {
        when(attendanceService.findById(99L))
                .thenThrow(new AttendanceEventNotFoundException(99L));

        mockMvc.perform(get(BASE + "/events/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PATCH /events/{id}/process
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_PROCESS")
    void processEvent_shouldReturn200_whenSuccess() throws Exception {
        when(attendanceService.findById(1L)).thenReturn(buildResponse(AttendanceEventStatus.PROCESSED));

        MarkProcessedRequest req = new MarkProcessedRequest();
        req.setProcessedBy("school");

        mockMvc.perform(patch(BASE + "/events/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));
    }

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_PROCESS")
    void processEvent_shouldReturn400_whenAlreadyProcessed() throws Exception {
        doThrow(new com.attendai.core.common.exception.ValidationException(
                "Only PENDING events can be marked as processed"))
                .when(attendanceService).markAsProcessed(anyLong(), anyString());

        MarkProcessedRequest req = new MarkProcessedRequest();
        req.setProcessedBy("school");

        mockMvc.perform(patch(BASE + "/events/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_READ")
    void processEvent_shouldReturn403_whenWrongPermission() throws Exception {
        MarkProcessedRequest req = new MarkProcessedRequest();
        req.setProcessedBy("school");

        mockMvc.perform(patch(BASE + "/events/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /events (list with filters)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_ATTENDANCE_READ")
    void listEvents_shouldReturn200_withNoFilters() throws Exception {
        when(attendanceService.findByFilters(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get(BASE + "/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private AttendanceEventResponse buildResponse(AttendanceEventStatus status) {
        return AttendanceEventResponse.builder()
                .id(1L)
                .personId(1L)
                .stationId(2L)
                .eventTime(LocalDateTime.now().minusMinutes(5))
                .direction(EventDirection.ENTRY)
                .source(EventSource.FACE_RECOGNITION)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
