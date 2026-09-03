package com.attendai.school.academiccalendar.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.academiccalendar.dto.CalendarEntryResponse;
import com.attendai.school.academiccalendar.dto.CreateHolidayRangeRequest;
import com.attendai.school.academiccalendar.dto.CreateHolidayRequest;
import com.attendai.school.academiccalendar.dto.DeclareWorkingDayRequest;
import com.attendai.school.academiccalendar.dto.MonthCalendarResponse;
import com.attendai.school.academiccalendar.dto.UpdateCalendarEntryRequest;
import com.attendai.school.academiccalendar.dto.WorkingDayCountResponse;
import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/calendar")
@RequiredArgsConstructor
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    // -------------------------------------------------------------------------
    // Create holiday (single day)
    // -------------------------------------------------------------------------

    @PostMapping("/holidays")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<ApiResponse<CalendarEntryResponse>> createHoliday(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody CreateHolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        calendarService.createHoliday(schoolId, academicYearId, request)));
    }

    // -------------------------------------------------------------------------
    // Create holiday range
    // -------------------------------------------------------------------------

    @PostMapping("/holidays/range")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponse>>> createHolidayRange(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody CreateHolidayRangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        calendarService.createHolidayRange(schoolId, academicYearId, request)));
    }

    // -------------------------------------------------------------------------
    // Update calendar entry
    // -------------------------------------------------------------------------

    @PutMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<ApiResponse<CalendarEntryResponse>> updateEntry(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long entryId,
            @Valid @RequestBody UpdateCalendarEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.updateEntry(schoolId, entryId, request)));
    }

    // -------------------------------------------------------------------------
    // Delete calendar entry
    // -------------------------------------------------------------------------

    @DeleteMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long entryId) {
        calendarService.deleteEntry(schoolId, entryId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Declare working day
    // -------------------------------------------------------------------------

    @PostMapping("/working-days")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<ApiResponse<CalendarEntryResponse>> declareWorkingDay(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody DeclareWorkingDayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        calendarService.declareWorkingDay(schoolId, academicYearId, request)));
    }

    // -------------------------------------------------------------------------
    // Convert holiday to working day
    // -------------------------------------------------------------------------

    @PatchMapping("/entries/{id}/convert-to-working-day")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_MANAGE')")
    public ResponseEntity<ApiResponse<CalendarEntryResponse>> convertToWorkingDay(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long entryId) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.convertToWorkingDay(schoolId, entryId)));
    }

    // -------------------------------------------------------------------------
    // Get month calendar
    // -------------------------------------------------------------------------

    @GetMapping("/month")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_READ')")
    public ResponseEntity<ApiResponse<MonthCalendarResponse>> getMonthCalendar(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @RequestParam("year")           int  year,
            @RequestParam("month")          int  month) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.getMonthCalendar(schoolId, academicYearId, year, month)));
    }

    // -------------------------------------------------------------------------
    // List calendar entries
    // -------------------------------------------------------------------------

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_READ')")
    public ResponseEntity<ApiResponse<List<CalendarEntryResponse>>> listEntries(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @RequestParam(name = "entryType", required = false) CalendarEntryType entryType,
            @RequestParam(name = "fromDate",  required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate",    required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.listEntries(schoolId, academicYearId, entryType, fromDate, toDate)));
    }

    // -------------------------------------------------------------------------
    // Get working day count
    // -------------------------------------------------------------------------

    @GetMapping("/working-days/count")
    @PreAuthorize("hasAuthority('SCHOOL_CALENDAR_READ')")
    public ResponseEntity<ApiResponse<WorkingDayCountResponse>> getWorkingDayCount(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @RequestParam("fromDate")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                calendarService.getWorkingDayCountResponse(schoolId, academicYearId, fromDate, toDate)));
    }
}
