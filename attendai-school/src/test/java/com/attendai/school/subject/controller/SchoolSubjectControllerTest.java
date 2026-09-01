package com.attendai.school.subject.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.subject.dto.CreateSubjectRequest;
import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import com.attendai.school.subject.exception.SubjectNotFoundException;
import com.attendai.school.subject.service.SchoolSubjectService;
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

@WebMvcTest(SchoolSubjectController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class SchoolSubjectControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean SchoolSubjectService subjectService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/subjects";

    // -------------------------------------------------------------------------
    // POST — create
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_CREATE")
    void createSubject_shouldReturn201_whenValid() throws Exception {
        when(subjectService.createSubject(anyLong(), any())).thenReturn(buildResponse());

        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setName("Mathematics");
        req.setCode("MATH");
        req.setType(SubjectType.ACADEMIC);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_CREATE")
    void createSubject_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"MATH\",\"type\":\"ACADEMIC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_CREATE")
    void createSubject_shouldReturn400_whenCodeMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathematics\",\"type\":\"ACADEMIC\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_CREATE")
    void createSubject_shouldReturn400_whenTypeMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathematics\",\"code\":\"MATH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_CREATE")
    void createSubject_shouldReturn409_whenNameDuplicate() throws Exception {
        when(subjectService.createSubject(anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException(
                        "Subject name 'Mathematics' already exists"));

        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setName("Mathematics");
        req.setCode("MATH");
        req.setType(SubjectType.ACADEMIC);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    void createSubject_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_READ")
    void createSubject_shouldReturn403_whenWrongPermission() throws Exception {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setName("Maths"); req.setCode("MATH"); req.setType(SubjectType.ACADEMIC);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_READ")
    void getSubject_shouldReturn200_whenFound() throws Exception {
        when(subjectService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.code").value("MATH"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_READ")
    void getSubject_shouldReturn404_whenNotFound() throws Exception {
        when(subjectService.findById(1L, 99L)).thenThrow(new SubjectNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getSubject_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET — list
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_READ")
    void listSubjects_shouldReturn200_whenNoFilter() throws Exception {
        when(subjectService.listSubjects(1L, null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_READ")
    void listSubjects_shouldReturn200_withTypeFilter() throws Exception {
        when(subjectService.listSubjects(1L, SubjectType.ACADEMIC, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE + "?type=ACADEMIC"))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PUT /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void updateSubject_shouldReturn200_whenValid() throws Exception {
        when(subjectService.updateSubject(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Advanced Maths\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/status
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void changeStatus_shouldReturn200_whenValid() throws Exception {
        when(subjectService.changeStatus(anyLong(), anyLong(), any())).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void changeStatus_shouldReturn400_whenStatusMissing() throws Exception {
        mockMvc.perform(patch(BASE + "/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /{id}/classes
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void assignToClass_shouldReturn200_whenValid() throws Exception {
        mockMvc.perform(post(BASE + "/1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void assignToClass_shouldReturn400_whenClassIdMissing() throws Exception {
        mockMvc.perform(post(BASE + "/1/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}/classes/{classId}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_UPDATE")
    void removeFromClass_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1/classes/2"))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_SUBJECT_DELETE")
    void deleteSubject_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSubject_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SubjectResponse buildResponse() {
        return SubjectResponse.builder()
                .id(1L).schoolId(1L).name("Mathematics").code("MATH")
                .type(SubjectType.ACADEMIC).status(SubjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
