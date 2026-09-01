package com.attendai.school.administrator.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.administrator.dto.AdministratorResponse;
import com.attendai.school.administrator.dto.CreateAdministratorRequest;
import com.attendai.school.administrator.entity.AdministratorStatus;
import com.attendai.school.administrator.exception.AdministratorNotFoundException;
import com.attendai.school.administrator.service.AdministratorService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdministratorController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AdministratorControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AdministratorService adminService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/administrators";

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_ADMINISTRATOR_CREATE")
    void createAdministrator_shouldReturn201_whenValid() throws Exception {
        when(adminService.createAdministrator(anyLong(), any())).thenReturn(buildResponse());

        CreateAdministratorRequest req = new CreateAdministratorRequest();
        req.setPersonId(10L);
        req.setUserId(20L);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_ADMINISTRATOR_CREATE")
    void createAdministrator_shouldReturn400_whenPersonIdMissing() throws Exception {
        CreateAdministratorRequest req = new CreateAdministratorRequest();
        req.setUserId(20L); // personId missing

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createAdministrator_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ADMINISTRATOR_READ")
    void createAdministrator_shouldReturn403_whenWrongPermission() throws Exception {
        CreateAdministratorRequest req = new CreateAdministratorRequest();
        req.setPersonId(10L); req.setUserId(20L);

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ADMINISTRATOR_READ")
    void getAdministrator_shouldReturn200_whenFound() throws Exception {
        when(adminService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ADMINISTRATOR_READ")
    void getAdministrator_shouldReturn404_whenNotFound() throws Exception {
        when(adminService.findById(1L, 99L)).thenThrow(new AdministratorNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_ADMINISTRATOR_UPDATE")
    void changeStatus_shouldReturn400_whenLastAdmin() throws Exception {
        when(adminService.changeStatus(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Cannot deactivate the last active administrator"));

        String body = "{\"status\":\"INACTIVE\"}";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_ADMINISTRATOR_DELETE")
    void deleteAdministrator_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    private AdministratorResponse buildResponse() {
        return AdministratorResponse.builder()
                .id(1L).schoolId(1L).personId(10L).userId(20L)
                .status(AdministratorStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
