package com.attendai.core.attendance.controller;

import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.dto.AttendanceEventSummaryResponse;
import com.attendai.core.attendance.dto.CorrectAttendanceEventRequest;
import com.attendai.core.attendance.dto.MarkProcessedRequest;
import com.attendai.core.attendance.dto.RecordAttendanceEventRequest;
import com.attendai.core.attendance.dto.RecordManualEventRequest;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import com.attendai.core.attendance.service.AttendanceService;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * REST controller for attendance event management.
 *
 * Base path: /api/v1/core/attendance
 *
 * Station-sourced events are authenticated via {@code X-Station-Api-Key}.
 * All other endpoints require a user JWT with the appropriate permission.
 */
@RestController
@RequestMapping("/api/v1/core/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * POST /api/v1/core/attendance/events
     * Record an attendance event from an authenticated station.
     * Authentication: X-Station-Api-Key (ROLE_STATION authority).
     */
    @PostMapping("/events")
    @PreAuthorize("hasAuthority('ROLE_STATION') or hasAuthority('CORE_ATTENDANCE_RECORD')")
    public ResponseEntity<ApiResponse<AttendanceEventResponse>> recordStationEvent(
            @Valid @RequestBody RecordAttendanceEventRequest request) {

        Long stationId = SecurityContextUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Valid station API key required"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendanceService.recordStationEvent(request, stationId)));
    }

    /**
     * POST /api/v1/core/attendance/events/manual
     * Manually record an attendance event by an operator.
     */
    @PostMapping("/events/manual")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_RECORD_MANUAL')")
    public ResponseEntity<ApiResponse<AttendanceEventResponse>> recordManualEvent(
            @Valid @RequestBody RecordManualEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendanceService.recordManualEvent(request)));
    }

    /**
     * GET /api/v1/core/attendance/events/{id}
     * Retrieve a single attendance event by ID.
     */
    @GetMapping("/events/{id}")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_READ')")
    public ResponseEntity<ApiResponse<AttendanceEventResponse>> getEvent(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.findById(id)));
    }

    /**
     * GET /api/v1/core/attendance/events
     * Query attendance events with optional filters.
     */
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_READ')")
    public ResponseEntity<PageResponse<AttendanceEventSummaryResponse>> listEvents(
            @RequestParam(name = "personId",  required = false) Long personId,
            @RequestParam(name = "stationId", required = false) Long stationId,
            @RequestParam(name = "status",    required = false) AttendanceEventStatus status,
            @RequestParam(name = "source",    required = false) EventSource source,
            @RequestParam(name = "direction", required = false) EventDirection direction,
            @RequestParam(name = "fromDate",  required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(name = "toDate",    required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Valid PageRequestParams pageParams) {

        return ResponseEntity.ok(PageResponse.of(
                attendanceService.findByFilters(personId, stationId, status, source,
                        direction, fromDate, toDate, pageParams.toPageable())));
    }

    /**
     * GET /api/v1/core/attendance/events/pending
     * Return PENDING events for business modules to consume.
     */
    @GetMapping("/events/pending")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_PROCESS')")
    public ResponseEntity<PageResponse<AttendanceEventResponse>> getPendingEvents(
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(attendanceService.findPending(pageParams.toPageable())));
    }

    /**
     * PATCH /api/v1/core/attendance/events/{id}/process
     * Mark a PENDING event as PROCESSED by a business module.
     */
    @PatchMapping("/events/{id}/process")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_PROCESS')")
    public ResponseEntity<ApiResponse<AttendanceEventResponse>> processEvent(
            @PathVariable("id") Long id,
            @Valid @RequestBody MarkProcessedRequest request) {

        attendanceService.markAsProcessed(id, request.getProcessedBy());
        return ResponseEntity.ok(ApiResponse.success(attendanceService.findById(id)));
    }

    /**
     * POST /api/v1/core/attendance/events/{id}/correct
     * Create a correction event linked to an existing event.
     */
    @PostMapping("/events/{id}/correct")
    @PreAuthorize("hasAuthority('CORE_ATTENDANCE_CORRECT')")
    public ResponseEntity<ApiResponse<AttendanceEventResponse>> correctEvent(
            @PathVariable("id") Long id,
            @Valid @RequestBody CorrectAttendanceEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(attendanceService.correctEvent(id, request)));
    }
}
