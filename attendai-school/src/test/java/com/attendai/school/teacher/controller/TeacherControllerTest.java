package com.attendai.school.teacher.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.teacher.dto.CreateTeacherRequest;
import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.entity.TeacherStatus;
import com.attendai.school.teacher.exception.TeacherNotFoundException;
import com.attendai.school.teacher.service.TeacherService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeacherController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class TeacherControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TeacherService teacherService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/teachers";

    // -------------------------------------------------------------------------
    // POST /teachers  — create
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_CREATE")
    void createTeacher_shouldReturn201_whenValid() throws Exception {
        when(teacherService.createTeacher(anyLong(), any())).thenReturn(buildResponse());

        CreateTeacherRequest req = new CreateTeacherRequest();
        req.setPersonId(10L);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_CREATE")
    void createTeacher_shouldReturn400_whenPersonIdMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createTeacher_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_READ")
    void createTeacher_shouldReturn403_whenWrongPermission() throws Exception {
        CreateTeacherRequest req = new CreateTeacherRequest();
        req.setPersonId(10L);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /teachers/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_READ")
    void getTeacher_shouldReturn200_whenFound() throws Exception {
        when(teacherService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_READ")
    void getTeacher_shouldReturn404_whenNotFound() throws Exception {
        when(teacherService.findById(1L, 99L)).thenThrow(new TeacherNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getTeacher_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PATCH /teachers/{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(teacherService.changeStatus(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_LEAVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void changeStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /teachers/{id}  — update
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void updateTeacher_shouldReturn200_whenValid() throws Exception {
        when(teacherService.updateTeacher(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"designation\":\"Senior Teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // -------------------------------------------------------------------------
    // POST /teachers/{id}/assign-user
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void assignUser_shouldReturn200_whenValid() throws Exception {
        when(teacherService.assignUser(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(post(BASE + "/1/assign-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":20}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void assignUser_shouldReturn400_whenUserIdMissing() throws Exception {
        mockMvc.perform(post(BASE + "/1/assign-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void assignUser_shouldReturn400_whenAlreadyLinked() throws Exception {
        when(teacherService.assignUser(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Teacher already has a user account linked."));

        mockMvc.perform(post(BASE + "/1/assign-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // DELETE /teachers/{id}/remove-user
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_UPDATE")
    void removeUser_shouldReturn200_whenSuccess() throws Exception {
        when(teacherService.removeUser(anyLong(), anyLong())).thenReturn(buildResponse());

        mockMvc.perform(delete(BASE + "/1/remove-user"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // DELETE /teachers/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_DELETE")
    void deleteTeacher_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTeacher_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TeacherResponse buildResponse() {
        return TeacherResponse.builder()
                .id(1L).schoolId(1L).personId(10L).userId(20L)
                .status(TeacherStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
