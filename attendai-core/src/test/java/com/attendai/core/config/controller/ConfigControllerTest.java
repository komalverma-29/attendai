package com.attendai.core.config.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.config.dto.SetConfigRequest;
import com.attendai.core.config.dto.SystemConfigResponse;
import com.attendai.core.config.entity.SystemConfig;
import com.attendai.core.config.exception.ConfigKeyNotFoundException;
import com.attendai.core.config.mapper.SystemConfigMapper;
import com.attendai.core.config.repository.SystemConfigRepository;
import com.attendai.core.config.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigController.class)
@Import(SecurityConfig.class)
class ConfigControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ConfigService            configService;
    @MockBean SystemConfigRepository   systemConfigRepository;
    @MockBean SystemConfigMapper       systemConfigMapper;

    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;

    private static final String BASE = "/api/v1/core/config";

    // -------------------------------------------------------------------------
    // GET /api/v1/core/config
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_READ")
    void listConfigs_shouldReturn200_whenAuthenticated() throws Exception {
        when(systemConfigRepository.findByFilters(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listConfigs_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_WRITE")
    void listConfigs_shouldReturn403_whenMissingReadPermission() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/core/config/{key}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_READ")
    void getConfig_shouldReturn200_whenKeyExists() throws Exception {
        SystemConfig entity = buildConfig("face.recognition.threshold", "0.85");
        SystemConfigResponse resp = buildResponse("face.recognition.threshold", "0.85");

        when(systemConfigRepository.findByConfigKey("face.recognition.threshold"))
                .thenReturn(Optional.of(entity));
        when(systemConfigMapper.toResponse(entity)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/face.recognition.threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configKey").value("face.recognition.threshold"))
                .andExpect(jsonPath("$.data.configValue").value("0.85"));
    }

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_READ")
    void getConfig_shouldReturn404_whenKeyNotFound() throws Exception {
        when(systemConfigRepository.findByConfigKey("missing.key")).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/missing.key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/core/config/{key}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_WRITE")
    void setConfig_shouldReturn200_whenValidRequest() throws Exception {
        SystemConfig entity = buildConfig("face.recognition.threshold", "0.90");
        SystemConfigResponse resp = buildResponse("face.recognition.threshold", "0.90");

        doNothing().when(configService).set(anyString(), anyString(), anyString(), any());
        when(systemConfigRepository.findByConfigKey("face.recognition.threshold"))
                .thenReturn(Optional.of(entity));
        when(systemConfigMapper.toResponse(entity)).thenReturn(resp);

        SetConfigRequest req = new SetConfigRequest();
        req.setValue("0.90");
        req.setModule("face");

        mockMvc.perform(put(BASE + "/face.recognition.threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("0.90"));
    }

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_WRITE")
    void setConfig_shouldReturn400_whenValueMissing() throws Exception {
        SetConfigRequest req = new SetConfigRequest();
        req.setModule("face");
        // value is missing

        mockMvc.perform(put(BASE + "/some.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_READ")
    void setConfig_shouldReturn403_whenMissingWritePermission() throws Exception {
        SetConfigRequest req = new SetConfigRequest();
        req.setValue("0.90");
        req.setModule("face");

        mockMvc.perform(put(BASE + "/some.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/core/config/{key}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_WRITE")
    void deleteConfig_shouldReturn200_whenKeyExists() throws Exception {
        when(configService.getString("face.threshold", "(not set)")).thenReturn("0.85");
        doNothing().when(configService).delete("face.threshold");

        mockMvc.perform(delete(BASE + "/face.threshold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Setting reset to default"));

        verify(configService).delete("face.threshold");
    }

    @Test
    @WithMockUser(authorities = "CORE_CONFIG_READ")
    void deleteConfig_shouldReturn403_whenMissingWritePermission() throws Exception {
        mockMvc.perform(delete(BASE + "/some.key"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SystemConfig buildConfig(String key, String value) {
        return SystemConfig.builder()
                .configKey(key)
                .configValue(value)
                .module("face")
                .build();
    }

    private SystemConfigResponse buildResponse(String key, String value) {
        return SystemConfigResponse.builder()
                .id(1L)
                .configKey(key)
                .configValue(value)
                .module("face")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
