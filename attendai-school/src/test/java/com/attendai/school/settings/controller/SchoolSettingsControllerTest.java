package com.attendai.school.settings.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.settings.dto.SchoolSettingResponse;
import com.attendai.school.settings.dto.SchoolSettingsSummaryResponse;
import com.attendai.school.settings.dto.SetSchoolSettingRequest;
import com.attendai.school.settings.service.SchoolSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolSettingsController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class SchoolSettingsControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SchoolSettingsService schoolSettingsService;

    // Beans required by SecurityConfig / StationSecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/settings";

    // -------------------------------------------------------------------------
    // GET /settings — list all
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SETTINGS_READ")
    void listSettings_shouldReturn200_withAllSettings() throws Exception {
        List<SchoolSettingsSummaryResponse> settings = List.of(
                SchoolSettingsSummaryResponse.builder()
                        .key("school.weekend.days").value("SAT,SUN")
                        .defaultValue("SAT,SUN").build(),
                SchoolSettingsSummaryResponse.builder()
                        .key("school.attendance.mark-absent.time").value("10:30")
                        .defaultValue("11:00").description("Earlier start").build()
        );
        when(schoolSettingsService.listSettings(1L)).thenReturn(settings);

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].key").value("school.weekend.days"))
                .andExpect(jsonPath("$.data[1].value").value("10:30"))
                .andExpect(jsonPath("$.data[1].defaultValue").value("11:00"));
    }

    @Test
    void listSettings_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SETTINGS_MANAGE")
    void listSettings_shouldReturn403_whenMissingReadPermission() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /settings/{key} — get single
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SETTINGS_READ")
    void getSetting_shouldReturn200_withEffectiveValue() throws Exception {
        SchoolSettingResponse resp = SchoolSettingResponse.builder()
                .key("school.weekend.days")
                .value("SAT,SUN")
                .defaultValue("SAT,SUN")
                .build();
        when(schoolSettingsService.getSetting(1L, "school.weekend.days")).thenReturn(resp);

        mockMvc.perform(get(BASE + "/school.weekend.days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.key").value("school.weekend.days"))
                .andExpect(jsonPath("$.data.value").value("SAT,SUN"));
    }

    // -------------------------------------------------------------------------
    // PUT /settings/{key} — set
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SETTINGS_MANAGE")
    void setSetting_shouldReturn200_whenValidKey() throws Exception {
        SchoolSettingResponse resp = SchoolSettingResponse.builder()
                .key("school.weekend.days").value("FRI,SAT")
                .defaultValue("SAT,SUN").build();
        when(schoolSettingsService.setSetting(eq(1L), eq("school.weekend.days"), any()))
                .thenReturn(resp);

        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("FRI,SAT");

        mockMvc.perform(put(BASE + "/school.weekend.days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("FRI,SAT"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SETTINGS_MANAGE")
    void setSetting_shouldReturn400_whenValueBlank() throws Exception {
        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("");   // blank — fails @NotBlank

        mockMvc.perform(put(BASE + "/school.weekend.days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SETTINGS_MANAGE")
    void setSetting_shouldReturn400_whenKeyIsUnrecognised() throws Exception {
        when(schoolSettingsService.setSetting(anyLong(), eq("unknown.key"), any()))
                .thenThrow(new ValidationException("Unknown school setting key: 'unknown.key'"));

        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("some-value");

        mockMvc.perform(put(BASE + "/unknown.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Unknown school setting key: 'unknown.key'"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SETTINGS_READ")
    void setSetting_shouldReturn403_whenMissingManagePermission() throws Exception {
        SetSchoolSettingRequest req = new SetSchoolSettingRequest();
        req.setValue("SAT,SUN");

        mockMvc.perform(put(BASE + "/school.weekend.days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // DELETE /settings/{key} — reset to default
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SETTINGS_MANAGE")
    void deleteSetting_shouldReturn200_withDefaultValue() throws Exception {
        when(schoolSettingsService.deleteSetting(1L, "school.weekend.days"))
                .thenReturn("SAT,SUN");

        mockMvc.perform(delete(BASE + "/school.weekend.days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Setting reset to default"))
                .andExpect(jsonPath("$.data.defaultValue").value("SAT,SUN"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SETTINGS_MANAGE")
    void deleteSetting_shouldReturn400_whenKeyIsUnrecognised() throws Exception {
        when(schoolSettingsService.deleteSetting(anyLong(), eq("unknown.key")))
                .thenThrow(new ValidationException("Unknown school setting key: 'unknown.key'"));

        mockMvc.perform(delete(BASE + "/unknown.key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SETTINGS_READ")
    void deleteSetting_shouldReturn403_whenMissingManagePermission() throws Exception {
        mockMvc.perform(delete(BASE + "/school.weekend.days"))
                .andExpect(status().isForbidden());
    }
}
