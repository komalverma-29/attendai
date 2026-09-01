package com.attendai.school.school.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.school.school.dto.CreateSchoolRequest;
import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import com.attendai.school.school.exception.SchoolNotFoundException;
import com.attendai.school.school.service.SchoolService;
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

@WebMvcTest(SchoolController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         com.attendai.core.common.handler.GlobalExceptionHandler.class})
class SchoolControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SchoolService schoolService;

    // Beans required by SecurityConfig / StationSecurityConfig
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools";

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SCHOOL_CREATE")
    void createSchool_shouldReturn201_whenValid() throws Exception {
        when(schoolService.createSchool(any())).thenReturn(buildResponse());

        CreateSchoolRequest req = new CreateSchoolRequest();
        req.setName("Sunrise Public School");
        req.setType(SchoolType.COMBINED);
        req.setAddressLine1("123 Main St");
        req.setCity("Mumbai");
        req.setStateOrProvince("MH");
        req.setCountry("IN");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Sunrise Public School"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SCHOOL_CREATE")
    void createSchool_shouldReturn400_whenNameMissing() throws Exception {
        CreateSchoolRequest req = new CreateSchoolRequest();
        req.setType(SchoolType.COMBINED);
        req.setAddressLine1("123");
        req.setCity("Mumbai");
        req.setStateOrProvince("MH");
        req.setCountry("IN");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSchool_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SCHOOL_READ")
    void createSchool_shouldReturn403_whenWrongPermission() throws Exception {
        CreateSchoolRequest req = new CreateSchoolRequest();
        req.setName("X"); req.setType(SchoolType.PRIMARY);
        req.setAddressLine1("A"); req.setCity("B"); req.setStateOrProvince("C"); req.setCountry("IN");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SCHOOL_READ")
    void getSchool_shouldReturn200_whenFound() throws Exception {
        when(schoolService.findById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SCHOOL_READ")
    void getSchool_shouldReturn404_whenNotFound() throws Exception {
        when(schoolService.findById(99L)).thenThrow(new SchoolNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "1", authorities = "SCHOOL_SCHOOL_DELETE")
    void deleteSchool_shouldReturn204() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SCHOOL_READ")
    void deleteSchool_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SchoolResponse buildResponse() {
        return SchoolResponse.builder()
                .id(1L).name("Sunrise Public School").code("SPS")
                .type(SchoolType.COMBINED).status(SchoolStatus.ACTIVE)
                .city("Mumbai").country("IN")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
