package com.attendai.core.attendance.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.attendance.config.AttendanceProperties;
import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.dto.AttendanceEventSummaryResponse;
import com.attendai.core.attendance.dto.CorrectAttendanceEventRequest;
import com.attendai.core.attendance.dto.RecordAttendanceEventRequest;
import com.attendai.core.attendance.dto.RecordManualEventRequest;
import com.attendai.core.attendance.entity.AttendanceEvent;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import com.attendai.core.attendance.exception.AttendanceEventNotFoundException;
import com.attendai.core.attendance.mapper.AttendanceEventMapper;
import com.attendai.core.attendance.repository.AttendanceEventRepository;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.station.service.StationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceEventRepository attendanceEventRepository;
    private final AttendanceEventMapper     attendanceEventMapper;
    private final AttendanceProperties      attendanceProperties;
    private final PersonService             personService;
    private final StationService            stationService;
    private final AuditService              auditService;

    // -------------------------------------------------------------------------
    // Record station event
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AttendanceEventResponse recordStationEvent(RecordAttendanceEventRequest request,
                                                       Long stationId) {
        // Validate person exists
        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException("Person with id " + request.getPersonId() + " was not found");
        }

        // Validate station is ACTIVE
        if (!stationService.isActiveById(stationId)) {
            return persistRejected(request.getPersonId(), stationId, request.getEventTime(),
                    request.getDirection(), request.getSource(), request.getNotes(),
                    "Station is not ACTIVE");
        }

        // Validate event time constraints
        validateEventTime(request.getEventTime());

        // Deduplication check
        boolean isDuplicate = isDuplicate(request.getPersonId(), stationId,
                request.getDirection(), request.getEventTime());

        AttendanceEventStatus status = isDuplicate
                ? AttendanceEventStatus.DUPLICATE
                : AttendanceEventStatus.PENDING;

        AttendanceEvent event = AttendanceEvent.builder()
                .personId(request.getPersonId())
                .stationId(stationId)
                .eventTime(request.getEventTime())
                .direction(request.getDirection())
                .source(request.getSource())
                .status(status)
                .notes(request.getNotes())
                .build();

        AttendanceEvent saved = attendanceEventRepository.save(event);

        String auditCode = isDuplicate ? "ATTENDANCE_EVENT_DUPLICATE" : "ATTENDANCE_EVENT_RECORDED";
        auditService.log(AuditEventRequest.builder()
                .actionCode(auditCode)
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("AttendanceEvent")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"personId\":" + saved.getPersonId()
                        + ",\"stationId\":" + saved.getStationId() + "}")
                .build());

        log.info("Station event recorded | eventId={} personId={} status={}",
                saved.getId(), saved.getPersonId(), saved.getStatus());

        return attendanceEventMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Record manual event
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AttendanceEventResponse recordManualEvent(RecordManualEventRequest request) {
        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException("Person with id " + request.getPersonId() + " was not found");
        }

        // Manual events do NOT validate event time against the future/past window
        // (operators may be recording a missed event) — but we keep a sanity check
        // that the time is not in the future
        if (request.getEventTime().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Manual event time must not be in the future");
        }

        AttendanceEvent event = AttendanceEvent.builder()
                .personId(request.getPersonId())
                .stationId(request.getStationId())
                .eventTime(request.getEventTime())
                .direction(request.getDirection())
                .source(EventSource.MANUAL)
                .status(AttendanceEventStatus.PENDING)
                .notes(request.getNotes())
                .build();

        AttendanceEvent saved = attendanceEventRepository.save(event);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_EVENT_MANUAL")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("AttendanceEvent")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"personId\":" + saved.getPersonId() + "}")
                .build());

        log.info("Manual event recorded | eventId={} personId={}", saved.getId(), saved.getPersonId());
        return attendanceEventMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Correct event
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AttendanceEventResponse correctEvent(Long originalEventId,
                                                 CorrectAttendanceEventRequest request) {
        // Ensure original event exists
        if (!attendanceEventRepository.existsById(originalEventId)) {
            throw new AttendanceEventNotFoundException(originalEventId);
        }

        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException("Person with id " + request.getPersonId() + " was not found");
        }

        AttendanceEvent correction = AttendanceEvent.builder()
                .personId(request.getPersonId())
                .eventTime(request.getEventTime())
                .direction(request.getDirection())
                .source(EventSource.CORRECTION)
                .status(AttendanceEventStatus.PENDING)
                .notes(request.getNotes())
                .originalEventId(originalEventId)
                .build();

        AttendanceEvent saved = attendanceEventRepository.save(correction);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_EVENT_CORRECTED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("AttendanceEvent")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"newEventId\":" + saved.getId()
                        + ",\"originalEventId\":" + originalEventId + "}")
                .build());

        log.info("Correction event created | newEventId={} originalEventId={}", saved.getId(), originalEventId);
        return attendanceEventMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AttendanceEventResponse findById(Long id) {
        return attendanceEventMapper.toResponse(requireEvent(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceEventSummaryResponse> findByFilters(
            Long personId, Long stationId,
            AttendanceEventStatus status, EventSource source, EventDirection direction,
            LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable) {
        return attendanceEventRepository
                .findByFilters(personId, stationId, status, source, direction, fromDate, toDate, pageable)
                .map(attendanceEventMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceEventResponse> findPending(Pageable pageable) {
        return attendanceEventRepository.findPending(pageable)
                .map(attendanceEventMapper::toResponse);
    }

    // -------------------------------------------------------------------------
    // Processing API
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void markAsProcessed(Long eventId, String processedBy) {
        AttendanceEvent event = requireEvent(eventId);

        if (event.getStatus() != AttendanceEventStatus.PENDING) {
            throw new ValidationException(
                    "Only PENDING events can be marked as processed. "
                    + "Event " + eventId + " is " + event.getStatus());
        }

        event.setStatus(AttendanceEventStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessedBy(processedBy);
        attendanceEventRepository.save(event);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_EVENT_PROCESSED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("AttendanceEvent")
                .resourceId(String.valueOf(eventId))
                .details("{\"processedBy\":\"" + processedBy + "\"}")
                .build());
    }

    // -------------------------------------------------------------------------
    // Internal API for business modules
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceEventResponse> findPendingEventsForPerson(Long personId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.plusDays(1).atStartOfDay();
        return attendanceEventRepository
                .findPendingByPersonAndDate(personId, dayStart, dayEnd)
                .stream()
                .map(attendanceEventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceEventResponse> findEventsByPersonAndDateRange(Long personId,
                                                                         LocalDateTime from,
                                                                         LocalDateTime to) {
        return attendanceEventRepository
                .findByPersonAndDateRange(personId, from, to)
                .stream()
                .map(attendanceEventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countEventsByPersonAndDate(Long personId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.plusDays(1).atStartOfDay();
        return attendanceEventRepository.countByPersonAndDate(personId, dayStart, dayEnd);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AttendanceEvent requireEvent(Long id) {
        return attendanceEventRepository.findById(id)
                .orElseThrow(() -> new AttendanceEventNotFoundException(id));
    }

    private void validateEventTime(LocalDateTime eventTime) {
        LocalDateTime now = LocalDateTime.now();

        if (eventTime.isAfter(now.plusSeconds(attendanceProperties.getMaxFutureSeconds()))) {
            throw new ValidationException(
                    "Event time is too far in the future (max "
                    + attendanceProperties.getMaxFutureSeconds() + "s tolerance)");
        }

        if (eventTime.isBefore(now.minusHours(attendanceProperties.getMaxPastHours()))) {
            throw new ValidationException(
                    "Event time is too far in the past (max "
                    + attendanceProperties.getMaxPastHours() + "h backdating allowed)");
        }
    }

    private boolean isDuplicate(Long personId, Long stationId,
                                 EventDirection direction, LocalDateTime eventTime) {
        LocalDateTime windowStart = eventTime.minusSeconds(attendanceProperties.getDedupWindowSeconds());
        List<AttendanceEvent> recent = attendanceEventRepository
                .findRecentForDeduplication(personId, stationId, direction, windowStart);
        return !recent.isEmpty();
    }

    private AttendanceEventResponse persistRejected(Long personId, Long stationId,
                                                      LocalDateTime eventTime,
                                                      EventDirection direction,
                                                      EventSource source, String notes,
                                                      String reason) {
        AttendanceEvent rejected = AttendanceEvent.builder()
                .personId(personId)
                .stationId(stationId)
                .eventTime(eventTime)
                .direction(direction)
                .source(source)
                .status(AttendanceEventStatus.REJECTED)
                .rejectionReason(reason)
                .notes(notes)
                .build();

        AttendanceEvent saved = attendanceEventRepository.save(rejected);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_EVENT_REJECTED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("AttendanceEvent")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"reason\":\"" + reason + "\"}")
                .build());

        log.warn("Attendance event rejected | eventId={} reason={}", saved.getId(), reason);
        return attendanceEventMapper.toResponse(saved);
    }
}
