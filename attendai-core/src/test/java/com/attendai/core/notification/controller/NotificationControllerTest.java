package com.attendai.core.notification.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.notification.dto.NotificationTemplateRequest;
import com.attendai.core.notification.dto.NotificationTemplateResponse;
import com.attendai.core.notification.entity.Channel;
import com.attendai.core.notification.mapper.NotificationMapper;
import com.attendai.core.notification.repository.InAppNotificationRepository;
import com.attendai.core.notification.repository.NotificationLogRepository;
import com.attendai.core.notification.repository.NotificationPreferenceRepository;
import com.attendai.core.notification.repository.NotificationTemplateRepository;
import com.attendai.core.station.config.StationSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, StationSecurityConfig.class})
class NotificationControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean InAppNotificationRepository      inAppRepository;
    @MockBean NotificationPreferenceRepository preferenceRepository;
    @MockBean NotificationTemplateRepository   templateRepository;
    @MockBean NotificationLogRepository        logRepository;
    @MockBean NotificationMapper               notificationMapper;

    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/core/notifications";

    // -------------------------------------------------------------------------
    // GET /inbox
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1")
    void getInbox_shouldReturn200_whenAuthenticated() throws Exception {
        when(inAppRepository.findInbox(any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE + "/inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getInbox_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/inbox"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /templates
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_NOTIFICATION_MANAGE")
    void listTemplates_shouldReturn200_whenAdmin() throws Exception {
        when(templateRepository.findAllByIsDeletedFalseOrderByTypeCodeAsc(any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE + "/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "1")
    void listTemplates_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get(BASE + "/templates"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // POST /templates
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_NOTIFICATION_MANAGE")
    void createTemplate_shouldReturn201_whenValidRequest() throws Exception {
        var saved = com.attendai.core.notification.entity.NotificationTemplate.builder()
                .typeCode("AUTH_PASSWORD_RESET").channel(Channel.EMAIL)
                .locale("en").subject("Reset").bodyTemplate("Hello {{name}}")
                .build();
        when(templateRepository.save(any())).thenReturn(saved);
        when(notificationMapper.toTemplateResponse(any())).thenReturn(
                NotificationTemplateResponse.builder()
                        .id(1L).typeCode("AUTH_PASSWORD_RESET")
                        .channel(Channel.EMAIL).locale("en")
                        .subject("Reset").bodyTemplate("Hello {{name}}")
                        .isActive(true).createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now()).build());

        NotificationTemplateRequest req = new NotificationTemplateRequest();
        req.setTypeCode("AUTH_PASSWORD_RESET");
        req.setChannel(Channel.EMAIL);
        req.setLocale("en");
        req.setSubject("Reset");
        req.setBodyTemplate("Hello {{name}}");

        mockMvc.perform(post(BASE + "/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.typeCode").value("AUTH_PASSWORD_RESET"));
    }

    @Test
    @WithMockUser(authorities = "CORE_NOTIFICATION_MANAGE")
    void createTemplate_shouldReturn400_whenTypeCodeMissing() throws Exception {
        NotificationTemplateRequest req = new NotificationTemplateRequest();
        req.setChannel(Channel.EMAIL);
        req.setLocale("en");
        req.setBodyTemplate("Hello");

        mockMvc.perform(post(BASE + "/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // GET /logs
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_NOTIFICATION_MANAGE")
    void getLogs_shouldReturn200_whenAdmin() throws Exception {
        when(logRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "1")
    void getLogs_shouldReturn403_whenNotAdmin() throws Exception {
        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isForbidden());
    }
}
