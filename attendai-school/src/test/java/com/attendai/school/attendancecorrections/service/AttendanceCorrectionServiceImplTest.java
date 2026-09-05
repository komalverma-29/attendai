package com.attendai.school.attendancecorrections.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancecorrections.dto.CreateCorrectionRequest;
import com.attendai.school.attendancecorrections.dto.ReviewCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.AttendanceCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.exception.CorrectionRequestNotFoundException;
import com.attendai.school.attendancecorrections.mapper.AttendanceCorrectionMapper;
import com.attendai.school.attendancecorrections.repository.AttendanceCorrectionRepository;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.settings.service.SchoolSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceCorrectionServiceImplTest {

    @Mock AttendanceCorrectionRepository correctionRepository;
    @Mock AttendanceCorrectionMapper     correctionMapper;
    @Mock DailyAttendanceRepository      attendanceRepository;
    @Mock DailyAttendanceService         attendanceService;
    @Mock AcademicYearService            academicYearService;
    @Mock AcademicCalendarService        calendarService;
    @Mock SchoolSettingsService          settingsService;
    @Mock AuditService                   auditService;

    private AttendanceCorrectionServiceImpl service;

    private static final Long   SCHOOL_ID  = 1L;
    private static final Long   STUDENT_ID = 30L;
    private static final Long   RECORD_ID  = 100L;
    private static final LocalDate PAST_DATE = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        service = new AttendanceCorrectionServiceImpl(
                correctionRepository, correctionMapper, attendanceRepository, attendanceService,
                academicYearService, calendarService, settingsService, auditService);
    }

    // =========================================================================
    // submitCorrection
    // =========================================================================

    @Test
    void submit_shouldSave_whenValid() {
        stubActiveYear();
        stubWorkingDay(true);
        stubAttendanceRecord(DailyAttendanceStatus.ABSENT);
        when(correctionRepository.existsByStudentIdAndAttendanceDateAndStatus(
                eq(STUDENT_ID), eq(PAST_DATE), eq(CorrectionStatus.PENDING))).thenReturn(false);
        AttendanceCorrectionRequest saved = buildCorrection(1L, CorrectionStatus.PENDING);
        when(correctionRepository.save(any())).thenReturn(saved);
        when(correctionMapper.toResponse(saved)).thenReturn(null);

        service.submitCorrection(SCHOOL_ID, buildRequest(DailyAttendanceStatus.PRESENT), 5L);

        verify(correctionRepository).save(any(AttendanceCorrectionRequest.class));
        verify(auditService).log(any());
    }

    @Test
    void submit_shouldThrow_whenOnLeaveRequested() {
        assertThatThrownBy(() -> service.submitCorrection(SCHOOL_ID,
                buildRequest(DailyAttendanceStatus.ON_LEAVE), 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ON_LEAVE");
    }

    @Test
    void submit_shouldThrow_whenFutureDate() {
        CreateCorrectionRequest req = buildRequest(DailyAttendanceStatus.PRESENT);
        req.setAttendanceDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.submitCorrection(SCHOOL_ID, req, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void submit_shouldThrow_whenNotWorkingDay() {
        stubActiveYear();
        stubWorkingDay(false);
        assertThatThrownBy(() -> service.submitCorrection(SCHOOL_ID,
                buildRequest(DailyAttendanceStatus.PRESENT), 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("working day");
    }

    @Test
    void submit_shouldThrow409_whenPendingAlreadyExists() {
        stubActiveYear();
        stubWorkingDay(true);
        stubAttendanceRecord(DailyAttendanceStatus.ABSENT);
        when(correctionRepository.existsByStudentIdAndAttendanceDateAndStatus(
                eq(STUDENT_ID), eq(PAST_DATE), eq(CorrectionStatus.PENDING))).thenReturn(true);
        assertThatThrownBy(() -> service.submitCorrection(SCHOOL_ID,
                buildRequest(DailyAttendanceStatus.PRESENT), 5L))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // =========================================================================
    // approveCorrection
    // =========================================================================

    @Test
    void approve_shouldApproveAndUpdateRecord() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.PENDING);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        when(settingsService.isFourEyesEnabled(SCHOOL_ID)).thenReturn(false);
        when(attendanceService.overrideAttendance(anyLong(), anyLong(), any(), any())).thenReturn(null);
        when(correctionRepository.save(any())).thenReturn(correction);
        when(correctionMapper.toResponse(any())).thenReturn(null);

        service.approveCorrection(SCHOOL_ID, 1L, new ReviewCorrectionRequest(), 99L);

        assertThat(correction.getStatus()).isEqualTo(CorrectionStatus.APPROVED);
        verify(attendanceService).overrideAttendance(eq(SCHOOL_ID), eq(RECORD_ID), any(), eq(99L));
    }

    @Test
    void approve_shouldThrow_whenFourEyesViolated() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.PENDING);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        when(settingsService.isFourEyesEnabled(SCHOOL_ID)).thenReturn(true);

        // requestedById = 5L (same as reviewer)
        assertThatThrownBy(() -> service.approveCorrection(SCHOOL_ID, 1L, null, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("four-eyes");
    }

    @Test
    void approve_shouldThrow_whenNotPending() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.APPROVED);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        assertThatThrownBy(() -> service.approveCorrection(SCHOOL_ID, 1L, null, 99L))
                .isInstanceOf(ValidationException.class);
    }

    // =========================================================================
    // rejectCorrection
    // =========================================================================

    @Test
    void reject_shouldReject_whenPending() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.PENDING);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        when(correctionRepository.save(any())).thenReturn(correction);
        when(correctionMapper.toResponse(any())).thenReturn(null);

        ReviewCorrectionRequest req = new ReviewCorrectionRequest();
        req.setRejectionReason("No evidence");
        service.rejectCorrection(SCHOOL_ID, 1L, req, 99L);

        assertThat(correction.getStatus()).isEqualTo(CorrectionStatus.REJECTED);
        assertThat(correction.getRejectionReason()).isEqualTo("No evidence");
    }

    // =========================================================================
    // cancelCorrection
    // =========================================================================

    @Test
    void cancel_shouldCancel_whenPending() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.PENDING);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        when(correctionRepository.save(any())).thenReturn(correction);
        when(correctionMapper.toResponse(any())).thenReturn(null);

        service.cancelCorrection(SCHOOL_ID, 1L, 5L);
        assertThat(correction.getStatus()).isEqualTo(CorrectionStatus.CANCELLED);
    }

    @Test
    void cancel_shouldThrow_whenAlreadyApproved() {
        AttendanceCorrectionRequest correction = buildCorrection(1L, CorrectionStatus.APPROVED);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(correction));
        assertThatThrownBy(() -> service.cancelCorrection(SCHOOL_ID, 1L, 5L))
                .isInstanceOf(ValidationException.class);
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Test
    void findById_shouldThrow404_whenBelongsToDifferentSchool() {
        AttendanceCorrectionRequest c = buildCorrection(1L, CorrectionStatus.PENDING);
        c.setSchoolId(99L);
        when(correctionRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.findById(SCHOOL_ID, 1L))
                .isInstanceOf(CorrectionRequestNotFoundException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubActiveYear() {
        when(academicYearService.getActiveAcademicYearOrThrow(SCHOOL_ID)).thenReturn(
                AcademicYearResponse.builder().id(10L).schoolId(SCHOOL_ID).name("2025-2026")
                        .startDate(LocalDate.of(2025, 6, 1)).endDate(LocalDate.of(2026, 3, 31))
                        .status(AcademicYearStatus.ACTIVE)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private void stubWorkingDay(boolean result) {
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), anyLong(), eq(PAST_DATE))).thenReturn(result);
    }

    private void stubAttendanceRecord(DailyAttendanceStatus status) {
        DailyAttendanceRecord record = DailyAttendanceRecord.builder()
                .schoolId(SCHOOL_ID).academicYearId(10L).sectionId(20L)
                .studentId(STUDENT_ID).attendanceDate(PAST_DATE).status(status).build();
        record.setId(RECORD_ID);
        when(attendanceRepository.findByStudentIdAndAttendanceDate(STUDENT_ID, PAST_DATE))
                .thenReturn(Optional.of(record));
    }

    private AttendanceCorrectionRequest buildCorrection(Long id, CorrectionStatus status) {
        AttendanceCorrectionRequest c = AttendanceCorrectionRequest.builder()
                .schoolId(SCHOOL_ID).academicYearId(10L).studentId(STUDENT_ID)
                .attendanceRecordId(RECORD_ID).attendanceDate(PAST_DATE)
                .originalStatus(DailyAttendanceStatus.ABSENT)
                .requestedStatus(DailyAttendanceStatus.PRESENT)
                .reason("Test").status(status).requestedById(5L).build();
        c.setId(id);
        return c;
    }

    private CreateCorrectionRequest buildRequest(DailyAttendanceStatus requestedStatus) {
        CreateCorrectionRequest req = new CreateCorrectionRequest();
        req.setStudentId(STUDENT_ID);
        req.setAttendanceDate(PAST_DATE);
        req.setRequestedStatus(requestedStatus);
        req.setReason("Station was offline");
        return req;
    }
}
