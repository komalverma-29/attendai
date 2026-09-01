package com.attendai.school.section.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.section.dto.CreateSectionRequest;
import com.attendai.school.section.dto.EnrollStudentInSectionRequest;
import com.attendai.school.section.dto.SectionEnrollmentResponse;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.exception.SectionEnrollmentNotFoundException;
import com.attendai.school.section.exception.SectionNotFoundException;
import com.attendai.school.section.service.SchoolSectionService;
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

@WebMvcTest(SchoolSectionController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class SchoolSectionControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SchoolSectionService sectionService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE =
            "/api/v1/school/schools/1/academic-years/10/classes/5/sections";

    // -------------------------------------------------------------------------
    // POST — create section
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_CREATE")
    void createSection_shouldReturn201_whenValid() throws Exception {
        when(sectionService.createSection(anyLong(), anyLong(), anyLong(), any()))
                .thenReturn(buildResponse());

        CreateSectionRequest req = new CreateSectionRequest();
        req.setName("A");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_CREATE")
    void createSection_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_CREATE")
    void createSection_shouldReturn409_whenNameDuplicate() throws Exception {
        when(sectionService.createSection(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Section 'A' already exists"));

        CreateSectionRequest req = new CreateSectionRequest();
        req.setName("A");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_CREATE")
    void createSection_shouldReturn400_whenYearCompleted() throws Exception {
        when(sectionService.createSection(anyLong(), anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Cannot create section for COMPLETED year"));

        CreateSectionRequest req = new CreateSectionRequest();
        req.setName("A");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createSection_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_READ")
    void createSection_shouldReturn403_whenWrongPermission() throws Exception {
        CreateSectionRequest req = new CreateSectionRequest();
        req.setName("A");
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET — list sections
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_READ")
    void listSections_shouldReturn200() throws Exception {
        when(sectionService.listSections(1L, 10L, 5L)).thenReturn(List.of());
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // GET /{sectionId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_READ")
    void getSection_shouldReturn200_whenFound() throws Exception {
        when(sectionService.findById(1L, 1L)).thenReturn(buildResponse());
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_READ")
    void getSection_shouldReturn404_whenNotFound() throws Exception {
        when(sectionService.findById(1L, 99L)).thenThrow(new SectionNotFoundException(99L));
        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getSection_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // PUT /{sectionId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_UPDATE")
    void updateSection_shouldReturn200_whenValid() throws Exception {
        when(sectionService.updateSection(anyLong(), anyLong(), any())).thenReturn(buildResponse());
        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A+\"}"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PATCH /{sectionId}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(sectionService.changeStatus(anyLong(), anyLong(), any())).thenReturn(buildResponse());
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_UPDATE")
    void changeStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /{sectionId}/students — enroll
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_MANAGE")
    void enrollStudent_shouldReturn201_whenValid() throws Exception {
        when(sectionService.enrollStudent(anyLong(), anyLong(), any()))
                .thenReturn(buildEnrollmentResponse());

        EnrollStudentInSectionRequest req = new EnrollStudentInSectionRequest();
        req.setStudentId(20L);
        req.setRollNumber("01");
        req.setEnrolledAt(LocalDate.now());

        mockMvc.perform(post(BASE + "/1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentId").value(20));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_MANAGE")
    void enrollStudent_shouldReturn400_whenStudentIdMissing() throws Exception {
        mockMvc.perform(post(BASE + "/1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rollNumber\":\"01\",\"enrolledAt\":\"2025-06-01\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_MANAGE")
    void enrollStudent_shouldReturn409_whenAlreadyEnrolled() throws Exception {
        when(sectionService.enrollStudent(anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Student already enrolled"));

        EnrollStudentInSectionRequest req = new EnrollStudentInSectionRequest();
        req.setStudentId(20L);
        req.setRollNumber("01");
        req.setEnrolledAt(LocalDate.now());

        mockMvc.perform(post(BASE + "/1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // -------------------------------------------------------------------------
    // GET /{sectionId}/students
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_READ")
    void listStudents_shouldReturn200() throws Exception {
        when(sectionService.getStudentsBySection(1L)).thenReturn(List.of());
        mockMvc.perform(get(BASE + "/1/students"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // DELETE /{sectionId}/students/{studentId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_MANAGE")
    void removeStudent_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1/students/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_MANAGE")
    void removeStudent_shouldReturn404_whenNotEnrolled() throws Exception {
        org.mockito.Mockito.doThrow(new SectionEnrollmentNotFoundException(20L, 1L))
                .when(sectionService).removeStudent(1L, 1L, 20L);

        mockMvc.perform(delete(BASE + "/1/students/20"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // DELETE /{sectionId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_DELETE")
    void deleteSection_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SECTION_DELETE")
    void deleteSection_shouldReturn400_whenStudentsEnrolled() throws Exception {
        org.mockito.Mockito.doThrow(new ValidationException("Section has enrolled students"))
                .when(sectionService).deleteSection(1L, 1L);

        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void deleteSection_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SectionResponse buildResponse() {
        return SectionResponse.builder()
                .id(1L).schoolId(1L).classId(5L).academicYearId(10L)
                .name("A").status(SectionStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private SectionEnrollmentResponse buildEnrollmentResponse() {
        return SectionEnrollmentResponse.builder()
                .id(1L).sectionId(1L).studentId(20L).academicYearId(10L)
                .rollNumber("01").enrolledAt(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
