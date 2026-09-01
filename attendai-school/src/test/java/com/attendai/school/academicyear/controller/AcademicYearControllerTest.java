package com.attendai.school.academicyear.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.dto.CreateAcademicYearRequest;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.exception.AcademicYearNotFoundException;
import com.attendai.school.academicyear.exception.ActiveAcademicYearAlreadyExistsException;
import com.attendai.school.academicyear.service.AcademicYearService;
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

@WebMvcTest(AcademicYearController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AcademicYearControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AcademicYearService academicYearService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE = "/api/v1/school/schools/1/academic-years";

    // -------------------------------------------------------------------------
    // POST — create
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_CREATE")
    void createAcademicYear_shouldReturn201_whenValid() throws Exception {
        when(academicYearService.createAcademicYear(anyLong(), any())).thenReturn(buildResponse());

        CreateAcademicYearRequest req = new CreateAcademicYearRequest();
        req.setName("2025-2026");
        req.setStartDate(LocalDate.of(2025, 6, 1));
        req.setEndDate(LocalDate.of(2026, 3, 31));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("UPCOMING"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_CREATE")
    void createAcademicYear_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-06-01\",\"endDate\":\"2026-03-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createAcademicYear_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_READ")
    void createAcademicYear_shouldReturn403_whenWrongPermission() throws Exception {
        CreateAcademicYearRequest req = new CreateAcademicYearRequest();
        req.setName("X");
        req.setStartDate(LocalDate.of(2025, 6, 1));
        req.setEndDate(LocalDate.of(2026, 3, 31));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // GET /active
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_READ")
    void getActiveAcademicYear_shouldReturn200_whenActive() throws Exception {
        when(academicYearService.getActiveAcademicYearOrThrow(1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_READ")
    void getActiveAcademicYear_shouldReturn404_whenNoneActive() throws Exception {
        when(academicYearService.getActiveAcademicYearOrThrow(1L))
                .thenThrow(new AcademicYearNotFoundException("No ACTIVE academic year"));

        mockMvc.perform(get(BASE + "/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // GET /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_READ")
    void getAcademicYear_shouldReturn200_whenFound() throws Exception {
        when(academicYearService.findById(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_READ")
    void getAcademicYear_shouldReturn404_whenNotFound() throws Exception {
        when(academicYearService.findById(1L, 99L))
                .thenThrow(new AcademicYearNotFoundException(99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/activate
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void activateAcademicYear_shouldReturn200_whenNoActiveExists() throws Exception {
        when(academicYearService.activateAcademicYear(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void activateAcademicYear_shouldReturn409_whenAnotherAlreadyActive() throws Exception {
        when(academicYearService.activateAcademicYear(1L, 1L))
                .thenThrow(new ActiveAcademicYearAlreadyExistsException(1L));

        mockMvc.perform(patch(BASE + "/1/activate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/complete
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void completeAcademicYear_shouldReturn200_whenActive() throws Exception {
        when(academicYearService.completeAcademicYear(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/complete"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void completeAcademicYear_shouldReturn400_whenNotActive() throws Exception {
        when(academicYearService.completeAcademicYear(1L, 1L))
                .thenThrow(new ValidationException("Only an ACTIVE year can be completed"));

        mockMvc.perform(patch(BASE + "/1/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // PATCH /{id}/cancel
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void cancelAcademicYear_shouldReturn200_whenUpcoming() throws Exception {
        when(academicYearService.cancelAcademicYear(1L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(patch(BASE + "/1/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void cancelAcademicYear_shouldReturn400_whenActive() throws Exception {
        when(academicYearService.cancelAcademicYear(1L, 1L))
                .thenThrow(new ValidationException("Only an UPCOMING year can be cancelled"));

        mockMvc.perform(patch(BASE + "/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // PUT /{id} — update
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void updateAcademicYear_shouldReturn200_whenValid() throws Exception {
        when(academicYearService.updateAcademicYear(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse());

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2025-2026 Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_UPDATE")
    void updateAcademicYear_shouldReturn400_whenCompletedYear() throws Exception {
        when(academicYearService.updateAcademicYear(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("A COMPLETED academic year is immutable"));

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE /{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = "SCHOOL_ACADEMIC_YEAR_DELETE")
    void deleteAcademicYear_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAcademicYear_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AcademicYearResponse buildResponse() {
        return AcademicYearResponse.builder()
                .id(1L).schoolId(1L).name("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(AcademicYearStatus.UPCOMING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
