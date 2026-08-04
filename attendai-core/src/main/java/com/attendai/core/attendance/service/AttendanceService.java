package com.attendai.core.attendance.service;

import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.dto.AttendanceEventSummaryResponse;
import com.attendai.core.attendance.dto.CorrectAttendanceEventRequest;
import com.attendai.core.attendance.dto.RecordAttendanceEventRequest;
import com.attendai.core.attendance.dto.RecordManualEventRequest;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core attendance event service.
 *
 * Exposes both the HTTP-facing write/read operations and an internal API
 * consumed directly by business modules (school, college, enterprise) as
 * Spring beans — not via HTTP.
 */
public interface AttendanceService {

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Records an attendance event submitted by a station.
     * Applies validation, deduplication, and persists with PENDING or DUPLICATE status.
     *
     * @param request   the event payload from the station
     * @param stationId the authenticated station's ID (from security context)
     */
    AttendanceEventResponse recordStationEvent(RecordAttendanceEventRequest request, Long stationId);

    /**
     * Records a manual attendance event created by an operator.
     * Bypasses deduplication. Source is always MANUAL.
     */
    AttendanceEventResponse recordManualEvent(RecordManualEventRequest request);

    /**
     * Creates a correction event linked to an existing event.
     * The original event is not modified; a new PENDING event with source=CORRECTION is created.
     */
    AttendanceEventResponse correctEvent(Long originalEventId, CorrectAttendanceEventRequest request);

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    AttendanceEventResponse findById(Long id);

    Page<AttendanceEventSummaryResponse> findByFilters(
            Long personId, Long stationId,
            AttendanceEventStatus status, EventSource source, EventDirection direction,
            LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable);

    Page<AttendanceEventResponse> findPending(Pageable pageable);

    // -------------------------------------------------------------------------
    // Processing API — consumed by business modules
    // -------------------------------------------------------------------------

    /**
     * Marks a PENDING event as PROCESSED. Only allowed on PENDING events.
     * Called by business modules after consuming the event.
     *
     * @param eventId     the event's surrogate ID
     * @param processedBy the module identifier, e.g. "school"
     */
    void markAsProcessed(Long eventId, String processedBy);

    // -------------------------------------------------------------------------
    // Internal API for business modules (called as Spring beans, not via HTTP)
    // -------------------------------------------------------------------------

    /**
     * Returns PENDING events for a person on a specific date (UTC).
     * Used by business modules during their attendance processing cycle.
     */
    List<AttendanceEventResponse> findPendingEventsForPerson(Long personId, LocalDate date);

    /**
     * Returns all events for a person within a datetime range (any status).
     */
    List<AttendanceEventResponse> findEventsByPersonAndDateRange(
            Long personId, LocalDateTime from, LocalDateTime to);

    /**
     * Counts PENDING or PROCESSED events for a person on a specific date.
     * Used by business modules for attendance percentage calculations.
     */
    int countEventsByPersonAndDate(Long personId, LocalDate date);
}
