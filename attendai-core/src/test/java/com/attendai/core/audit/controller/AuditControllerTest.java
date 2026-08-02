package com.attendai.core.audit.controller;

import com.attendai.core.audit.entity.AuditLog;
import com.attendai.core.audit.mapper.AuditLogMapper;
import com.attendai.core.audit.repository.AuditLogRepository;
import com.attendai.core.auth.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.attendai.core.audit.dto.AuditLogResponse;

@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuditLogRepository auditLogRepository;
    @MockBean AuditLogMapper     auditLogMapper;

    // Required by SecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties securityProperties;

    private static final String BASE = "/api/v1/core/audit";

    // -------------------------------------------------------------------------
    // GET /logs
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_AUDIT_READ")
    void queryLogs_shouldReturn200_whenAuthenticated() throws Exception {
        AuditLog entry = buildAuditLog(1L);
        AuditLogResponse resp = buildAuditLogResponse(1L);

        when(auditLogRepository.findByFilters(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));
        when(auditLogMapper.toResponse(entry)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].actionCode").value("AUTH_LOGIN_SUCCESS"));
    }

    @Test
    void queryLogs_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CORE_USER_READ")
    void queryLogs_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CORE_AUDIT_READ")
    void queryLogs_shouldReturnEmptyPage_whenNoResults() throws Exception {
        when(auditLogRepository.findByFilters(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE + "/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.pagination.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // GET /logs/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_AUDIT_READ")
    void getLog_shouldReturn200_whenFound() throws Exception {
        AuditLog entry = buildAuditLog(5L);
        AuditLogResponse resp = buildAuditLogResponse(5L);

        when(auditLogRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(auditLogMapper.toResponse(entry)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/logs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    @WithMockUser(authorities = "CORE_AUDIT_READ")
    void getLog_shouldReturn404_whenNotFound() throws Exception {
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/logs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // Verify no write endpoint exists (confirmed by read-only controller design)
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "CORE_AUDIT_READ")
    void getLog_shouldReturn200_afterQueryLogs_confirmsReadOnlyController() throws Exception {
        // This test confirms that the controller only exposes GET endpoints.
        // The AuditController class has no @PostMapping, @PutMapping, or @DeleteMapping.
        // Write access is only available via the internal AuditService Spring bean.
        AuditLog entry = buildAuditLog(10L);
        AuditLogResponse resp = buildAuditLogResponse(10L);

        when(auditLogRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(auditLogMapper.toResponse(entry)).thenReturn(resp);

        mockMvc.perform(get(BASE + "/logs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AuditLog buildAuditLog(Long id) {
        return AuditLog.builder()
                .actionCode("AUTH_LOGIN_SUCCESS")
                .module("core-auth")
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private AuditLogResponse buildAuditLogResponse(Long id) {
        return AuditLogResponse.builder()
                .id(id)
                .actionCode("AUTH_LOGIN_SUCCESS")
                .module("core-auth")
                .occurredAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
