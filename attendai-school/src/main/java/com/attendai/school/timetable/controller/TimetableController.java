package com.attendai.school.timetable.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.timetable.dto.CreateTimeSlotRequest;
import com.attendai.school.timetable.dto.CreateTimetableEntryRequest;
import com.attendai.school.timetable.dto.SectionTimetableResponse;
import com.attendai.school.timetable.dto.TimeSlotResponse;
import com.attendai.school.timetable.dto.TimetableEntryResponse;
import com.attendai.school.timetable.dto.UpdateTimetableEntryRequest;
import com.attendai.school.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    // =========================================================================
    // Time Slots — /api/v1/school/schools/{schoolId}/time-slots
    // =========================================================================

    @PostMapping("/api/v1/school/schools/{schoolId}/time-slots")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_MANAGE')")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> createTimeSlot(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateTimeSlotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(timetableService.createTimeSlot(schoolId, request)));
    }

    @GetMapping("/api/v1/school/schools/{schoolId}/time-slots")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_READ')")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> listTimeSlots(
            @PathVariable("schoolId") Long schoolId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.listTimeSlots(schoolId)));
    }

    @DeleteMapping("/api/v1/school/schools/{schoolId}/time-slots/{timeSlotId}")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_MANAGE')")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable("schoolId")   Long schoolId,
            @PathVariable("timeSlotId") Long timeSlotId) {
        timetableService.deleteTimeSlot(schoolId, timeSlotId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Timetable Entries — /api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable
    // =========================================================================

    @PostMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable/entries")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_MANAGE')")
    public ResponseEntity<ApiResponse<TimetableEntryResponse>> createEntry(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody CreateTimetableEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        timetableService.createEntry(schoolId, academicYearId, request)));
    }

    @GetMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable/sections/{sectionId}")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_READ')")
    public ResponseEntity<ApiResponse<SectionTimetableResponse>> getSectionTimetable(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("sectionId")      Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getSectionTimetable(schoolId, sectionId, academicYearId)));
    }

    @GetMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable/teachers/{teacherId}")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_READ')")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getTeacherTimetable(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("teacherId")      Long teacherId) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.getTeacherTimetable(schoolId, teacherId, academicYearId)));
    }

    @PutMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable/entries/{entryId}")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_MANAGE')")
    public ResponseEntity<ApiResponse<TimetableEntryResponse>> updateEntry(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("entryId")  Long entryId,
            @Valid @RequestBody UpdateTimetableEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                timetableService.updateEntry(schoolId, entryId, request)));
    }

    @DeleteMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/timetable/entries/{entryId}")
    @PreAuthorize("hasAuthority('SCHOOL_TIMETABLE_MANAGE')")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("entryId")  Long entryId) {
        timetableService.deleteEntry(schoolId, entryId);
        return ResponseEntity.noContent().build();
    }
}
