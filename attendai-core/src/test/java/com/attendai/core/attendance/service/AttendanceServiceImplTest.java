package com.attendai.core.attendance.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.attendance.config.AttendanceProperties;
import com.attendai.core.attendance.dto.AttendanceEventResponse;
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
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.station.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock AttendanceEventRepository attendanceEventRepository;
    @Mock AttendanceEventMapper     attendanceEventMapper;
    @Mock PersonService             personService;
    @Mock StationService            stationService;
    @Mock AuditService              auditService;

    private AttendanceProperties attendanceProperties;
    private AttendanceServiceImpl service;

    @BeforeEach
    void setUp() {
        attendanceProperties = new AttendanceProperties();
        // dedup=5min, future=60s, past=24h
        service = new AttendanceServiceImpl(
                attendanceEventRepository, attendanceEventMapper,
                attendanceProperties, personService, stationService, auditService);
    }

    // -------------------------------------------------------------------------
    // recordStationEvent — happy path
    // -------------------------------------------------------------------------

    @Test
    void recordStationEvent_shouldReturnPendingEvent_whenValid() {
        when(personService.existsById(1L)).thenReturn(true);
        when(stationService.isActiveById(2L)).thenReturn(true);
        when(attendanceEventRepository.findRecentForDeduplication(any(), any(), any(), any()))
                .thenReturn(List.of());  // no duplicate

        AttendanceEvent saved = buildEvent(AttendanceEventStatus.PENDING);
        when(attendanceEventRepository.save(any())).thenReturn(saved);
        when(attendanceEventMapper.toResponse(saved)).thenReturn(buildResponse(AttendanceEventStatus.PENDING));

        RecordAttendanceEventRequest req = buildStationRequest();
        AttendanceEventResponse result = service.recordStationEvent(req, 2L);

        assertThat(result.getStatus()).isEqualTo(AttendanceEventStatus.PENDING);
        verify(attendanceEventRepository).save(any(AttendanceEvent.class));
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    @Test
    void recordStationEvent_shouldReturnDuplicate_whenWithinDedupWindow() {
        when(personService.existsById(1L)).thenReturn(true);
        when(stationService.isActiveById(2L)).thenReturn(true);
        // Simulate existing event within the dedup window
        when(attendanceEventRepository.findRecentForDeduplication(any(), any(), any(), any()))
                .thenReturn(List.of(buildEvent(AttendanceEventStatus.PENDING)));

        AttendanceEvent dupEvent = buildEvent(AttendanceEventStatus.DUPLICATE);
        when(attendanceEventRepository.save(any())).thenReturn(dupEvent);
        when(attendanceEventMapper.toResponse(dupEvent)).thenReturn(buildResponse(AttendanceEventStatus.DUPLICATE));

        AttendanceEventResponse result = service.recordStationEvent(buildStationRequest(), 2L);

        assertThat(result.getStatus()).isEqualTo(AttendanceEventStatus.DUPLICATE);
    }

    // -------------------------------------------------------------------------
    // Station not active → REJECTED
    // -------------------------------------------------------------------------

    @Test
    void recordStationEvent_shouldReturnRejected_whenStationNotActive() {
        when(personService.existsById(1L)).thenReturn(true);
        when(stationService.isActiveById(2L)).thenReturn(false);

        AttendanceEvent rejectedEvent = buildEvent(AttendanceEventStatus.REJECTED);
        when(attendanceEventRepository.save(any())).thenReturn(rejectedEvent);
        when(attendanceEventMapper.toResponse(rejectedEvent))
                .thenReturn(buildResponse(AttendanceEventStatus.REJECTED));

        AttendanceEventResponse result = service.recordStationEvent(buildStationRequest(), 2L);

        assertThat(result.getStatus()).isEqualTo(AttendanceEventStatus.REJECTED);
        // Dedup check must NOT run for rejected events
        verify(attendanceEventRepository, never()).findRecentForDeduplication(any(), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // Event time validation
    // -------------------------------------------------------------------------

    @Test
    void recordStationEvent_shouldThrow400_whenEventTimeTooFarInFuture() {
        when(personService.existsById(1L)).thenReturn(true);
        when(stationService.isActiveById(2L)).thenReturn(true);

        RecordAttendanceEventRequest req = buildStationRequest();
        req.setEventTime(LocalDateTime.now().plusMinutes(5)); // 5 min > 60s tolerance

        assertThatThrownBy(() -> service.recordStationEvent(req, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void recordStationEvent_shouldThrow400_whenEventTimeTooFarInPast() {
        when(personService.existsById(1L)).thenReturn(true);
        when(stationService.isActiveById(2L)).thenReturn(true);

        RecordAttendanceEventRequest req = buildStationRequest();
        req.setEventTime(LocalDateTime.now().minusHours(25)); // 25h > 24h limit

        assertThatThrownBy(() -> service.recordStationEvent(req, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("past");
    }

    @Test
    void recordStationEvent_shouldThrow404_whenPersonNotFound() {
        when(personService.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.recordStationEvent(buildStationRequest(), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(attendanceEventRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // recordManualEvent
    // -------------------------------------------------------------------------

    @Test
    void recordManualEvent_shouldBypassDeduplication() {
        when(personService.existsById(1L)).thenReturn(true);

        AttendanceEvent saved = buildEvent(AttendanceEventStatus.PENDING);
        when(attendanceEventRepository.save(any())).thenReturn(saved);
        when(attendanceEventMapper.toResponse(saved)).thenReturn(buildResponse(AttendanceEventStatus.PENDING));

        RecordManualEventRequest req = new RecordManualEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().minusHours(1));
        req.setDirection(EventDirection.ENTRY);

        service.recordManualEvent(req);

        // Deduplication must NOT be called for manual events
        verify(attendanceEventRepository, never()).findRecentForDeduplication(any(), any(), any(), any());
        verify(attendanceEventRepository).save(any());
    }

    @Test
    void recordManualEvent_shouldThrow400_whenEventTimeIsInFuture() {
        when(personService.existsById(1L)).thenReturn(true);

        RecordManualEventRequest req = new RecordManualEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().plusHours(1));
        req.setDirection(EventDirection.ENTRY);

        assertThatThrownBy(() -> service.recordManualEvent(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future");
    }

    // -------------------------------------------------------------------------
    // markAsProcessed
    // -------------------------------------------------------------------------

    @Test
    void markAsProcessed_shouldTransitionPendingToProcessed() {
        AttendanceEvent event = buildEvent(AttendanceEventStatus.PENDING);
        when(attendanceEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendanceEventRepository.save(any())).thenReturn(event);

        service.markAsProcessed(1L, "school");

        assertThat(event.getStatus()).isEqualTo(AttendanceEventStatus.PROCESSED);
        assertThat(event.getProcessedBy()).isEqualTo("school");
        assertThat(event.getProcessedAt()).isNotNull();
        verify(auditService).log(any());
    }

    @Test
    void markAsProcessed_shouldThrow400_whenEventIsAlreadyProcessed() {
        AttendanceEvent event = buildEvent(AttendanceEventStatus.PROCESSED);
        when(attendanceEventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.markAsProcessed(1L, "school"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void markAsProcessed_shouldThrow404_whenEventNotFound() {
        when(attendanceEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsProcessed(99L, "school"))
                .isInstanceOf(AttendanceEventNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // correctEvent
    // -------------------------------------------------------------------------

    @Test
    void correctEvent_shouldCreateNewPendingEventWithCorrectionSource() {
        when(attendanceEventRepository.existsById(5L)).thenReturn(true);
        when(personService.existsById(1L)).thenReturn(true);

        AttendanceEvent correction = buildEvent(AttendanceEventStatus.PENDING);
        when(attendanceEventRepository.save(any())).thenReturn(correction);
        when(attendanceEventMapper.toResponse(correction)).thenReturn(buildResponse(AttendanceEventStatus.PENDING));

        CorrectAttendanceEventRequest req = new CorrectAttendanceEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().minusHours(1));
        req.setDirection(EventDirection.ENTRY);

        AttendanceEventResponse result = service.correctEvent(5L, req);

        assertThat(result).isNotNull();
        verify(attendanceEventRepository).save(any(AttendanceEvent.class));
        verify(auditService).log(any());
    }

    @Test
    void correctEvent_shouldThrow404_whenOriginalEventNotFound() {
        when(attendanceEventRepository.existsById(99L)).thenReturn(false);

        CorrectAttendanceEventRequest req = new CorrectAttendanceEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now());
        req.setDirection(EventDirection.ENTRY);

        assertThatThrownBy(() -> service.correctEvent(99L, req))
                .isInstanceOf(AttendanceEventNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RecordAttendanceEventRequest buildStationRequest() {
        RecordAttendanceEventRequest req = new RecordAttendanceEventRequest();
        req.setPersonId(1L);
        req.setEventTime(LocalDateTime.now().minusMinutes(2)); // within valid window
        req.setDirection(EventDirection.ENTRY);
        req.setSource(EventSource.FACE_RECOGNITION);
        return req;
    }

    private AttendanceEvent buildEvent(AttendanceEventStatus status) {
        return AttendanceEvent.builder()
                .personId(1L)
                .stationId(2L)
                .eventTime(LocalDateTime.now().minusMinutes(2))
                .direction(EventDirection.ENTRY)
                .source(EventSource.FACE_RECOGNITION)
                .status(status)
                .build();
    }

    private AttendanceEventResponse buildResponse(AttendanceEventStatus status) {
        return AttendanceEventResponse.builder()
                .id(1L)
                .personId(1L)
                .stationId(2L)
                .eventTime(LocalDateTime.now())
                .direction(EventDirection.ENTRY)
                .source(EventSource.FACE_RECOGNITION)
                .status(status)
                .build();
    }
}
