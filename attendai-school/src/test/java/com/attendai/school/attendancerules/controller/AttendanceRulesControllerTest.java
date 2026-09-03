package com.attendai.school.attendancerules.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.attendancerules.dto.AttendanceRuleSetResponse;
import com.attendai.school.attendancerules.dto.CreateRuleSetRequest;
import com.attendai.school.attendancerules.dto.CreateSectionOverrideRequest;
import com.attendai.school.attendancerules.dto.EffectiveRulesResponse;
import com.attendai.school.attendancerules.dto.SectionOverrideResponse;
import com.attendai.school.attendancerules.exception.AttendanceRuleSetNotFoundException;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AttendanceRulesController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AttendanceRulesControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AttendanceRulesService rulesService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE =
            "/api/v1/school/schools/1/academic-years/10/attendance-rules";

    // =========================================================================
    // POST — create rule set
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createRuleSet_shouldReturn201_whenValid() throws Exception {
        when(rulesService.createRuleSet(anyLong(), anyLong(), any()))
                .thenReturn(buildRuleSetResponse());

        CreateRuleSetRequest req = new CreateRuleSetRequest();
        req.setLateThresholdTime(LocalTime.of(9, 15));
        req.setMinAttendancePercentage(new BigDecimal("75.00"));
        req.setConsecutiveAbsenceAlert(3);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createRuleSet_shouldReturn400_whenLateTimeMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minAttendancePercentage\":75.00,\"consecutiveAbsenceAlert\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createRuleSet_shouldReturn400_whenPercentageMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateThresholdTime\":\"09:15\",\"consecutiveAbsenceAlert\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createRuleSet_shouldReturn409_whenDuplicate() throws Exception {
        when(rulesService.createRuleSet(anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Rule set already exists"));

        CreateRuleSetRequest req = new CreateRuleSetRequest();
        req.setLateThresholdTime(LocalTime.of(9, 0));
        req.setMinAttendancePercentage(new BigDecimal("75.00"));
        req.setConsecutiveAbsenceAlert(3);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    void createRuleSet_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_READ")
    void createRuleSet_shouldReturn403_whenWrongPermission() throws Exception {
        CreateRuleSetRequest req = new CreateRuleSetRequest();
        req.setLateThresholdTime(LocalTime.of(9, 0));
        req.setMinAttendancePercentage(new BigDecimal("75.00"));
        req.setConsecutiveAbsenceAlert(3);
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET — get rule set
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_READ")
    void getRuleSet_shouldReturn200_whenFound() throws Exception {
        when(rulesService.getRuleSet(1L, 10L)).thenReturn(buildRuleSetResponse());
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_READ")
    void getRuleSet_shouldReturn404_whenNotFound() throws Exception {
        when(rulesService.getRuleSet(1L, 10L))
                .thenThrow(new AttendanceRuleSetNotFoundException(1L, 10L));
        mockMvc.perform(get(BASE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getRuleSet_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // PUT — update rule set
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void updateRuleSet_shouldReturn200_whenValid() throws Exception {
        when(rulesService.updateRuleSet(anyLong(), anyLong(), any()))
                .thenReturn(buildRuleSetResponse());
        mockMvc.perform(put(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateThresholdTime\":\"09:15\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void updateRuleSet_shouldReturn404_whenNotFound() throws Exception {
        when(rulesService.updateRuleSet(anyLong(), anyLong(), any()))
                .thenThrow(new AttendanceRuleSetNotFoundException(1L, 10L));
        mockMvc.perform(put(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /sections/{sectionId}/override
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createSectionOverride_shouldReturn201_whenValid() throws Exception {
        when(rulesService.createSectionOverride(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(buildOverrideResponse());
        mockMvc.perform(post(BASE + "/sections/20/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lateThresholdTime\":\"08:45\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sectionId").value(20));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createSectionOverride_shouldReturn400_whenSectionBelongsToDifferentSchool() throws Exception {
        when(rulesService.createSectionOverride(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Section does not belong to school"));
        mockMvc.perform(post(BASE + "/sections/20/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void createSectionOverride_shouldReturn409_whenDuplicate() throws Exception {
        when(rulesService.createSectionOverride(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Override already exists"));
        mockMvc.perform(post(BASE + "/sections/20/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    // =========================================================================
    // DELETE /sections/{sectionId}/override
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void deleteSectionOverride_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/sections/20/override"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSectionOverride_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/sections/20/override"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // GET /sections/{sectionId}/effective
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_READ")
    void getEffectiveRules_shouldReturn200_whenFound() throws Exception {
        EffectiveRulesResponse resp = EffectiveRulesResponse.builder()
                .schoolId(1L).academicYearId(10L).sectionId(20L)
                .lateThresholdTime(LocalTime.of(9, 0))
                .minAttendancePercentage(new BigDecimal("75.00"))
                .consecutiveAbsenceAlert(3).fromRuleSet(true).build();
        when(rulesService.getEffectiveRules(1L, 10L, 20L)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/sections/20/effective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(20))
                .andExpect(jsonPath("$.data.consecutiveAbsenceAlert").value(3));
    }

    @Test
    void getEffectiveRules_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20/effective"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ATTENDANCE_RULES_MANAGE")
    void getEffectiveRules_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/sections/20/effective"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AttendanceRuleSetResponse buildRuleSetResponse() {
        return AttendanceRuleSetResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L)
                .lateThresholdTime(LocalTime.of(9, 15))
                .minAttendancePercentage(new BigDecimal("75.00"))
                .consecutiveAbsenceAlert(3)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private SectionOverrideResponse buildOverrideResponse() {
        return SectionOverrideResponse.builder()
                .id(1L).ruleSetId(1L).sectionId(20L)
                .lateThresholdTime(LocalTime.of(8, 45))
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
