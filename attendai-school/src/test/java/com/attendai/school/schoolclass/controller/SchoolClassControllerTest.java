package com.attendai.school.schoolclass.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.dto.CreateClassRequest;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.exception.ClassNotFoundException;
import com.attendai.school.schoolclass.service.SchoolClassService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolClassController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class SchoolClassControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SchoolClassService schoolClassService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/classes";

    // -------------------------------------------------------------------------
    // POST — create
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_CREATE")
    void createClass_shouldReturn201_whenValid() throws Exception {
        when(schoolClassService.createClass(anyLong(), any())).thenReturn(buildResponse());

        CreateClassRequest req = new CreateClassRequest();
        req.setName("Grade 5");
        req.setGradeOrder(5);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_CREATE")
    void createClass_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradeOrder\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_CREATE")
    void createClass_shouldReturn400_whenGradeOrderMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grade 5\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_CREATE")
    void createClass_shouldReturn409_whenNameDuplicate() throws Exception {
        when(schoolClassService.createClass(anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Class 'Grade 5' already exists"));

        CreateClassRequest req = new CreateClassRequest();
        req.setName("Grade 5");
        req.setGradeOrder(5);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    void createClass_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_READ")
    void createClass_shouldReturn403_whenWrongPermission() throws Exception {
        CreateClassRequest req = new CreateClassRequest();
        req.setName("Grade 5");
        req.setGradeOrder(5);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_READ")
    void getClass_shouldReturn200_whenFound() throws Exception {
        when(schoolClassService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_READ")
    void getClass_shouldReturn404_whenNotFound() throws Exception {
        when(schoolClassService.findById(1L, 99L)).thenThrow(new ClassNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getClass_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET — list
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_READ")
    void listClasses_shouldReturn200_whenNoFilter() throws Exception {
        when(schoolClassService.listClasses(1L, null)).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PUT /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_UPDATE")
    void updateClass_shouldReturn200_whenValid() throws Exception {
        when(schoolClassService.updateClass(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Grade Five\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(schoolClassService.changeStatus(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_UPDATE")
    void changeStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_CLASS_DELETE")
    void deleteClass_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteClass_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ClassResponse buildResponse() {
        return ClassResponse.builder()
                .id(1L).schoolId(1L).name("Grade 5").gradeOrder(5)
                .status(ClassStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
