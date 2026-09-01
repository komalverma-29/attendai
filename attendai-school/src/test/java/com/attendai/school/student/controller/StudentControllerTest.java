package com.attendai.school.student.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.student.dto.EnrollStudentRequest;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.entity.StudentStatus;
import com.attendai.school.student.exception.StudentNotFoundException;
import com.attendai.school.student.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
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

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class StudentControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean StudentService studentService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/students";

    // -------------------------------------------------------------------------
    // POST /students  — enroll
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_CREATE")
    void enrollStudent_shouldReturn201_whenValid() throws Exception {
        when(studentService.enrollStudent(anyLong(), any())).thenReturn(buildResponse());

        EnrollStudentRequest req = new EnrollStudentRequest();
        req.setPersonId(10L);
        req.setAdmissionNumber("ADM-001");
        req.setEnrollmentDate(LocalDate.now());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_CREATE")
    void enrollStudent_shouldReturn400_whenPersonIdMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_CREATE")
    void enrollStudent_shouldReturn400_whenAdmissionNumberMissing() throws Exception {
        String body = "{\"personId\":10,\"enrollmentDate\":\"2024-01-01\"}";
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enrollStudent_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_READ")
    void enrollStudent_shouldReturn403_whenWrongPermission() throws Exception {
        EnrollStudentRequest req = new EnrollStudentRequest();
        req.setPersonId(10L);
        req.setAdmissionNumber("ADM-001");
        req.setEnrollmentDate(LocalDate.now());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /students/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_READ")
    void getStudent_shouldReturn200_whenFound() throws Exception {
        when(studentService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_READ")
    void getStudent_shouldReturn404_whenNotFound() throws Exception {
        when(studentService.findById(1L, 99L)).thenThrow(new StudentNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getStudent_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PATCH /students/{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(studentService.changeStatus(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void changeStatus_shouldReturn400_whenTerminalState() throws Exception {
        when(studentService.changeStatus(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Cannot change status of a student in terminal state"));

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void changeStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /students/{id}  — update
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void updateStudent_shouldReturn200_whenValid() throws Exception {
        when(studentService.updateStudent(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guardianName\":\"Jane Doe\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // -------------------------------------------------------------------------
    // POST /students/{id}/assign-user
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void assignUser_shouldReturn200_whenValid() throws Exception {
        when(studentService.assignUser(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(post(BASE + "/1/assign-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":20}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void assignUser_shouldReturn400_whenUserIdMissing() throws Exception {
        mockMvc.perform(post(BASE + "/1/assign-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /students/{id}/remove-user
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_UPDATE")
    void removeUser_shouldReturn200_whenSuccess() throws Exception {
        when(studentService.removeUser(anyLong(), anyLong())).thenReturn(buildResponse());

        mockMvc.perform(delete(BASE + "/1/remove-user"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // DELETE /students/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_STUDENT_DELETE")
    void deleteStudent_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteStudent_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private StudentResponse buildResponse() {
        return StudentResponse.builder()
                .id(1L).schoolId(1L).personId(10L)
                .admissionNumber("ADM-001")
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
