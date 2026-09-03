package com.attendai.school.academiccalendar.controller;

import com.attendai.core.auth.config.SecurityConfig;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.handler.GlobalExceptionHandler;
import com.attendai.school.academiccalendar.dto.CalendarEntryResponse;
import com.attendai.school.academiccalendar.dto.CreateHolidayRequest;
import com.attendai.school.academiccalendar.dto.MonthCalendarResponse;
import com.attendai.school.academiccalendar.dto.WorkingDayCountResponse;
import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import com.attendai.school.academiccalendar.exception.CalendarEntryNotFoundException;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcademicCalendarController.class)
@Import({SecurityConfig.class,
         com.attendai.core.station.config.StationSecurityConfig.class,
         GlobalExceptionHandler.class})
class AcademicCalendarControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AcademicCalendarService calendarService;
    @MockBean com.attendai.core.auth.service.JwtService        jwtService;
    @MockBean com.attendai.core.auth.config.SecurityProperties  securityProperties;
    @MockBean com.attendai.core.station.service.StationService  stationService;

    private static final String BASE =
            "/api/v1/school/schools/1/academic-years/10/calendar";

    // =========================================================================
    // POST /holidays
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHoliday_shouldReturn201_whenValid() throws Exception {
        when(calendarService.createHoliday(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(CalendarEntryType.HOLIDAY));

        CreateHolidayRequest req = new CreateHolidayRequest();
        req.setDate(LocalDate.of(2025, 10, 24));
        req.setName("Dussehra");

        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.entryType").value("HOLIDAY"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHoliday_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2025-10-24\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHoliday_shouldReturn400_whenDateMissing() throws Exception {
        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dussehra\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHoliday_shouldReturn409_whenDuplicateDate() throws Exception {
        when(calendarService.createHoliday(anyLong(), anyLong(), any()))
                .thenThrow(new ResourceAlreadyExistsException("Entry already exists"));

        CreateHolidayRequest req = new CreateHolidayRequest();
        req.setDate(LocalDate.of(2025, 10, 24)); req.setName("Dussehra");

        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHoliday_shouldReturn400_whenDateOutsideYear() throws Exception {
        when(calendarService.createHoliday(anyLong(), anyLong(), any()))
                .thenThrow(new ValidationException("Date outside academic year range"));

        CreateHolidayRequest req = new CreateHolidayRequest();
        req.setDate(LocalDate.of(2024, 1, 1)); req.setName("Old");

        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createHoliday_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_READ")
    void createHoliday_shouldReturn403_whenWrongPermission() throws Exception {
        CreateHolidayRequest req = new CreateHolidayRequest();
        req.setDate(LocalDate.of(2025, 10, 24)); req.setName("X");
        mockMvc.perform(post(BASE + "/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /holidays/range
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHolidayRange_shouldReturn201_whenValid() throws Exception {
        when(calendarService.createHolidayRange(anyLong(), anyLong(), any()))
                .thenReturn(List.of(buildResponse(CalendarEntryType.HOLIDAY)));

        mockMvc.perform(post(BASE + "/holidays/range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-10-24\",\"endDate\":\"2025-10-26\",\"name\":\"Break\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].entryType").value("HOLIDAY"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void createHolidayRange_shouldReturn400_whenNameMissing() throws Exception {
        mockMvc.perform(post(BASE + "/holidays/range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-10-24\",\"endDate\":\"2025-10-26\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PUT /entries/{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void updateEntry_shouldReturn200_whenValid() throws Exception {
        when(calendarService.updateEntry(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(CalendarEntryType.HOLIDAY));
        mockMvc.perform(put(BASE + "/entries/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void updateEntry_shouldReturn404_whenNotFound() throws Exception {
        when(calendarService.updateEntry(anyLong(), anyLong(), any()))
                .thenThrow(new CalendarEntryNotFoundException(99L));
        mockMvc.perform(put(BASE + "/entries/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    // =========================================================================
    // DELETE /entries/{id}
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void deleteEntry_shouldReturn204_whenSuccess() throws Exception {
        mockMvc.perform(delete(BASE + "/entries/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEntry_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete(BASE + "/entries/1"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // POST /working-days
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void declareWorkingDay_shouldReturn201_whenValid() throws Exception {
        when(calendarService.declareWorkingDay(anyLong(), anyLong(), any()))
                .thenReturn(buildResponse(CalendarEntryType.WORKING_DAY));
        mockMvc.perform(post(BASE + "/working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2025-11-08\",\"name\":\"Makeup Saturday\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entryType").value("WORKING_DAY"));
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void declareWorkingDay_shouldReturn400_whenDateMissing() throws Exception {
        mockMvc.perform(post(BASE + "/working-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Makeup\"}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PATCH /entries/{id}/convert-to-working-day
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void convertToWorkingDay_shouldReturn200() throws Exception {
        when(calendarService.convertToWorkingDay(anyLong(), anyLong()))
                .thenReturn(buildResponse(CalendarEntryType.WORKING_DAY));
        mockMvc.perform(patch(BASE + "/entries/1/convert-to-working-day"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entryType").value("WORKING_DAY"));
    }

    // =========================================================================
    // GET /month
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_READ")
    void getMonthCalendar_shouldReturn200() throws Exception {
        MonthCalendarResponse resp = MonthCalendarResponse.builder()
                .year(2025).month(10).days(List.of()).build();
        when(calendarService.getMonthCalendar(anyLong(), anyLong(), anyInt(), anyInt()))
                .thenReturn(resp);
        mockMvc.perform(get(BASE + "/month?year=2025&month=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2025))
                .andExpect(jsonPath("$.data.month").value(10));
    }

    @Test
    void getMonthCalendar_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/month?year=2025&month=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_MANAGE")
    void getMonthCalendar_shouldReturn403_whenWrongPermission() throws Exception {
        mockMvc.perform(get(BASE + "/month?year=2025&month=10"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /entries
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_READ")
    void listEntries_shouldReturn200() throws Exception {
        when(calendarService.listEntries(anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(List.of(buildResponse(CalendarEntryType.HOLIDAY)));
        mockMvc.perform(get(BASE + "/entries"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GET /working-days/count
    // =========================================================================

    @Test
    @WithMockUser(authorities = "SCHOOL_CALENDAR_READ")
    void getWorkingDayCount_shouldReturn200() throws Exception {
        WorkingDayCountResponse resp = WorkingDayCountResponse.builder()
                .fromDate(LocalDate.of(2025, 6, 1))
                .toDate(LocalDate.of(2025, 10, 31))
                .workingDayCount(98).build();
        when(calendarService.getWorkingDayCountResponse(anyLong(), anyLong(), any(), any()))
                .thenReturn(resp);
        mockMvc.perform(get(BASE + "/working-days/count"
                            + "?fromDate=2025-06-01&toDate=2025-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workingDayCount").value(98));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private CalendarEntryResponse buildResponse(CalendarEntryType type) {
        return CalendarEntryResponse.builder()
                .id(1L).schoolId(1L).academicYearId(10L)
                .entryDate(LocalDate.of(2025, 10, 24))
                .entryType(type).name("Holiday")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }
}
