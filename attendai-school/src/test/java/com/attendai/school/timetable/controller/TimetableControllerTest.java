package com.attendai.school.timetable.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.timetable.dto.CreateTimeSlotRequest;
import com.attendai.school.timetable.dto.CreateTimetableEntryRequest;
import com.attendai.school.timetable.dto.SectionTimetableResponse;
import com.attendai.school.timetable.dto.TimeSlotResponse;
import com.attendai.school.timetable.dto.TimetableEntryResponse;
import com.attendai.school.timetable.entity.TimeSlotType;
import com.attendai.school.timetable.exception.TimetableEntryNotFoundException;
import com.attendai.school.timetable.service.TimetableService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimetableController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class TimetableControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TimetableService timetableService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String SLOTS_BASE   = "/api/v1/school/schools/1/time-slots";
    private static final String TT_BASE      =
            "/api/v1/school/schools/1/academic-years/10/timetable";

    // =========================================================================
    // POST /time-slots
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createTimeSlot_shouldReturn201_whenValid() throws Exception {
        when(timetableService.createTimeSlot(anyLong(), any()))
                .thenReturn(buildSlotResponse());

        CreateTimeSlotRequest req = new CreateTimeSlotRequest();
        req.setName("Period 1"); req.setStartTime(LocalTime.of(9,0));
        req.setEndTime(LocalTime.of(9,45)); req.setSlotOrder(1);

        mockMvc.perform(post(SLOTS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createTimeSlot_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(SLOTS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"09:00\",\"endTime\":\"09:45\",\"slotOrder\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createTimeSlot_shouldReturn409_whenNameDuplicate() throws Exception {
        when(timetableService.createTimeSlot(anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Time slot already exists"));

        CreateTimeSlotRequest req = new CreateTimeSlotRequest();
        req.setName("Period 1"); req.setStartTime(LocalTime.of(9,0));
        req.setEndTime(LocalTime.of(9,45)); req.setSlotOrder(1);

        mockMvc.perform(post(SLOTS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    void createTimeSlot_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(SLOTS_BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_READ")
    void createTimeSlot_shouldReturn403_whenWrongPermission() throws Exception {
        CreateTimeSlotRequest req = new CreateTimeSlotRequest();
        req.setName("P1"); req.setStartTime(LocalTime.of(9,0));
        req.setEndTime(LocalTime.of(9,45)); req.setSlotOrder(1);
        mockMvc.perform(post(SLOTS_BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /time-slots
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_READ")
    void listTimeSlots_shouldReturn200() throws Exception {
        when(timetableService.listTimeSlots(1L)).thenReturn(List.of(buildSlotResponse()));
        mockMvc.perform(get(SLOTS_BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Period 1"));
    }

    // =========================================================================
    // DELETE /time-slots/{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void deleteTimeSlot_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(SLOTS_BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    // =========================================================================
    // POST /timetable/entries
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createEntry_shouldReturn201_whenValid() throws Exception {
        when(timetableService.createEntry(anyLong(), anyLong(), any()))
                .thenReturn(buildEntryResponse());

        CreateTimetableEntryRequest req = new CreateTimetableEntryRequest();
        req.setSectionId(20L); req.setTimeSlotId(5L);
        req.setDayOfWeek(DayOfWeek.MONDAY); req.setAssignmentId(10L);

        mockMvc.perform(post(TT_BASE + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createEntry_shouldReturn400_whenSectionIdMissing() throws Exception {
        mockMvc.perform(post(TT_BASE + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"timeSlotId\":5,\"dayOfWeek\":\"MONDAY\",\"assignmentId\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createEntry_shouldReturn409_whenSectionSlotConflict() throws Exception {
        when(timetableService.createEntry(anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Entry already exists"));

        CreateTimetableEntryRequest req = new CreateTimetableEntryRequest();
        req.setSectionId(20L); req.setTimeSlotId(5L);
        req.setDayOfWeek(DayOfWeek.MONDAY); req.setAssignmentId(10L);

        mockMvc.perform(post(TT_BASE + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void createEntry_shouldReturn400_whenAssignmentInactive() throws Exception {
        when(timetableService.createEntry(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Assignment is not ACTIVE"));

        CreateTimetableEntryRequest req = new CreateTimetableEntryRequest();
        req.setSectionId(20L); req.setTimeSlotId(5L);
        req.setDayOfWeek(DayOfWeek.MONDAY); req.setAssignmentId(10L);

        mockMvc.perform(post(TT_BASE + "/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createEntry_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(TT_BASE + "/entries")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /timetable/sections/{sectionId}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_READ")
    void getSectionTimetable_shouldReturn200() throws Exception {
        SectionTimetableResponse response = SectionTimetableResponse.builder()
                .sectionId(20L).academicYearId(10L).schedule(Map.of())
                .build();
        when(timetableService.getSectionTimetable(1L, 20L, 10L)).thenReturn(response);

        mockMvc.perform(get(TT_BASE + "/sections/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(20));
    }

    @Test
    void getSectionTimetable_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(TT_BASE + "/sections/20"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /timetable/teachers/{teacherId}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_READ")
    void getTeacherTimetable_shouldReturn200() throws Exception {
        when(timetableService.getTeacherTimetable(1L, 40L, 10L)).thenReturn(List.of());
        mockMvc.perform(get(TT_BASE + "/teachers/40"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // PUT /timetable/entries/{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void updateEntry_shouldReturn200_whenValid() throws Exception {
        when(timetableService.updateEntry(anyLong(), anyLong(), any()))
                .thenReturn(buildEntryResponse());

        mockMvc.perform(put(TT_BASE + "/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":11}"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // DELETE /timetable/entries/{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void deleteEntry_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(TT_BASE + "/entries/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TIMETABLE_MANAGE")
    void deleteEntry_shouldReturn404_whenNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new TimetableEntryNotFoundException(99L))
                .when(timetableService).deleteEntry(1L, 99L);

        mockMvc.perform(delete(TT_BASE + "/entries/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TimeSlotResponse buildSlotResponse() {
        return TimeSlotResponse.builder()
                .id(1L).schoolId(1L).name("Period 1")
                .startTime(LocalTime.of(9,0)).endTime(LocalTime.of(9,45))
                .slotOrder(1).slotType(TimeSlotType.PERIOD)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TimetableEntryResponse buildEntryResponse() {
        return TimetableEntryResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L).sectionId(20L)
                .timeSlotId(5L).dayOfWeek(DayOfWeek.MONDAY).assignmentId(10L)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
