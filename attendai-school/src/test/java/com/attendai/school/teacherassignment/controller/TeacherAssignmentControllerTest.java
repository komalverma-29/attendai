package com.attendai.school.teacherassignment.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.teacherassignment.dto.CreateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentResponse;
import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.exception.TeacherAssignmentNotFoundException;
import com.attendai.school.teacherassignment.service.TeacherAssignmentService;
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

@WebMvcTest(TeacherAssignmentController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class TeacherAssignmentControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean TeacherAssignmentService assignmentService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE =
            "/api/v1/school/schools/1/academic-years/10/assignments";

    // -------------------------------------------------------------------------
    // POST — create
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_CREATE")
    void createAssignment_shouldReturn201_whenValid() throws Exception {
        when(assignmentService.createAssignment(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse());

        CreateTeacherAssignmentRequest req = new CreateTeacherAssignmentRequest();
        req.setSectionId(20L); req.setSubjectId(30L); req.setTeacherId(40L);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_CREATE")
    void createAssignment_shouldReturn400_whenSectionIdMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":30,\"teacherId\":40}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_CREATE")
    void createAssignment_shouldReturn409_whenDuplicateAssignment() throws Exception {
        when(assignmentService.createAssignment(anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Assignment already exists"));

        CreateTeacherAssignmentRequest req = new CreateTeacherAssignmentRequest();
        req.setSectionId(20L); req.setSubjectId(30L); req.setTeacherId(40L);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_CREATE")
    void createAssignment_shouldReturn400_whenTeacherNotActive() throws Exception {
        when(assignmentService.createAssignment(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Teacher 40 is not ACTIVE"));

        CreateTeacherAssignmentRequest req = new CreateTeacherAssignmentRequest();
        req.setSectionId(20L); req.setSubjectId(30L); req.setTeacherId(40L);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createAssignment_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_READ")
    void createAssignment_shouldReturn403_whenWrongPermission() throws Exception {
        CreateTeacherAssignmentRequest req = new CreateTeacherAssignmentRequest();
        req.setSectionId(20L); req.setSubjectId(30L); req.setTeacherId(40L);
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET — list
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_READ")
    void listAssignments_shouldReturn200() throws Exception {
        when(assignmentService.listAssignments(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_READ")
    void listAssignments_shouldReturn200_withSectionFilter() throws Exception {
        when(assignmentService.listAssignments(1L, 10L, 20L, null, null))
                .thenReturn(List.of());
        mockMvc.perform(get(BASE + "?sectionId=20"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_READ")
    void getAssignment_shouldReturn200_whenFound() throws Exception {
        when(assignmentService.findById(1L, 1L)).thenReturn(buildResponse());
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_READ")
    void getAssignment_shouldReturn404_whenNotFound() throws Exception {
        when(assignmentService.findById(1L, 99L))
                .thenThrow(new TeacherAssignmentNotFoundException(99L));
        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getAssignment_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PUT /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_UPDATE")
    void updateAssignment_shouldReturn200_whenValid() throws Exception {
        when(assignmentService.updateAssignment(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse());
        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":50}"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(assignmentService.changeStatus(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse());
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_UPDATE")
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
    @WithMockUser(authorities = "SCHOOL_TEACHER_ASSIGNMENT_DELETE")
    void deleteAssignment_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAssignment_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TeacherAssignmentResponse buildResponse() {
        return TeacherAssignmentResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L)
                .sectionId(20L).subjectId(30L).teacherId(40L)
                .classTeacher(false).status(AssignmentStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
