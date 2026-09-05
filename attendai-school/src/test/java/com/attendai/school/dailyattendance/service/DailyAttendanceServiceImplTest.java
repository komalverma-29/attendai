package com.attendai.school.dailyattendance.service;

import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.entity.AttendanceEventStatus;
import com.attendai.core.attendance.entity.EventDirection;
import com.attendai.core.attendance.entity.EventSource;
import com.attendai.core.attendance.service.AttendanceService;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.notification.service.NotificationService;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.exception.AttendanceRecordNotFoundException;
import com.attendai.school.dailyattendance.mapper.DailyAttendanceMapper;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.section.entity.SectionEnrollment;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.settings.service.SchoolSettingsService;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.entity.StudentStatus;
import com.attendai.school.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyAttendanceServiceImplTest {

    @Mock DailyAttendanceRepository   attendanceRepository;
    @Mock DailyAttendanceMapper       attendanceMapper;
    @Mock AttendanceService           coreAttendanceService;
    @Mock AcademicYearService         academicYearService;
    @Mock AcademicCalendarService     calendarService;
    @Mock AttendanceRulesService      rulesService;
    @Mock StudentService              studentService;
    @Mock SchoolSectionService        sectionService;
    @Mock SectionEnrollmentRepository enrollmentRepository;
    @Mock SchoolSettingsService       settingsService;
    @Mock NotificationService         notificationService;
    @Mock AuditService                auditService;

    private DailyAttendanceServiceImpl service;

    private static final Long   SCHOOL_ID  = 1L;
    private static final Long   YEAR_ID    = 10L;
    private static final Long   SECTION_ID = 20L;
    private static final Long   STUDENT_ID = 30L;
    private static final Long   PERSON_ID  = 99L;
    private static final LocalDate TODAY   = LocalDate.of(2025, 10, 8); // Wednesday

    @BeforeEach
    void setUp() {
        service = new DailyAttendanceServiceImpl(
                attendanceRepository, attendanceMapper, coreAttendanceService,
                academicYearService, calendarService, rulesService,
                studentService, sectionService, enrollmentRepository,
                settingsService, notificationService, auditService);
    }

    // =========================================================================
    // processSchoolAttendanceEvents
    // =========================================================================

    @Test
    void processEvents_shouldMarkPresent_whenArrivalBeforeThreshold() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));

        StudentResponse student = buildStudentResponse(STUDENT_ID, SCHOOL_ID, PERSON_ID);
        when(studentService.findById(eq(SCHOOL_ID), eq(STUDENT_ID), eq(false))).thenReturn(student);
        when(studentService.isActive(STUDENT_ID)).thenReturn(true);

        AttendanceEventResponse event = buildEvent(1L, PERSON_ID,
                LocalDateTime.of(2025, 10, 8, 8, 45));
        when(coreAttendanceService.findPendingEventsForPerson(eq(PERSON_ID), any()))
                .thenReturn(List.of(event));
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(eq(STUDENT_ID), any()))
                .thenReturn(false);
        when(rulesService.getLateThreshold(SECTION_ID, YEAR_ID))
                .thenReturn(LocalTime.of(9, 0));
        when(attendanceRepository.save(any())).thenAnswer(inv -> {
            DailyAttendanceRecord r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verify(attendanceRepository).save(argThat(r ->
                r.getStatus() == DailyAttendanceStatus.PRESENT));
        verify(coreAttendanceService).markAsProcessed(1L, "school");
        verify(auditService).log(any());
    }

    @Test
    void processEvents_shouldMarkLate_whenArrivalAfterThreshold() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));

        StudentResponse student = buildStudentResponse(STUDENT_ID, SCHOOL_ID, PERSON_ID);
        when(studentService.findById(eq(SCHOOL_ID), eq(STUDENT_ID), eq(false))).thenReturn(student);
        when(studentService.isActive(STUDENT_ID)).thenReturn(true);

        AttendanceEventResponse event = buildEvent(2L, PERSON_ID,
                LocalDateTime.of(2025, 10, 8, 9, 30));
        when(coreAttendanceService.findPendingEventsForPerson(eq(PERSON_ID), any()))
                .thenReturn(List.of(event));
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(eq(STUDENT_ID), any()))
                .thenReturn(false);
        when(rulesService.getLateThreshold(SECTION_ID, YEAR_ID))
                .thenReturn(LocalTime.of(9, 0));
        when(attendanceRepository.save(any())).thenAnswer(inv -> {
            DailyAttendanceRecord r = inv.getArgument(0);
            r.setId(101L);
            return r;
        });

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verify(attendanceRepository).save(argThat(r ->
                r.getStatus() == DailyAttendanceStatus.LATE));
    }

    @Test
    void processEvents_shouldSkip_whenNotWorkingDay() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(false);

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verifyNoInteractions(enrollmentRepository, attendanceRepository);
    }

    @Test
    void processEvents_shouldSkip_whenNoActiveYear() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID)).thenReturn(Optional.empty());

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verifyNoInteractions(enrollmentRepository, calendarService, attendanceRepository);
    }

    @Test
    void processEvents_shouldBeIdempotent_whenRecordAlreadyExists() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));

        StudentResponse student = buildStudentResponse(STUDENT_ID, SCHOOL_ID, PERSON_ID);
        when(studentService.findById(eq(SCHOOL_ID), eq(STUDENT_ID), eq(false))).thenReturn(student);
        when(studentService.isActive(STUDENT_ID)).thenReturn(true);

        AttendanceEventResponse event = buildEvent(3L, PERSON_ID,
                LocalDateTime.of(2025, 10, 8, 8, 45));
        when(coreAttendanceService.findPendingEventsForPerson(eq(PERSON_ID), any()))
                .thenReturn(List.of(event));
        // Record already exists — idempotent
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(eq(STUDENT_ID), any()))
                .thenReturn(true);

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verify(attendanceRepository, never()).save(any());
        verify(coreAttendanceService).markAsProcessed(3L, "school");
    }

    @Test
    void processEvents_shouldSkipInactiveStudent() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));

        StudentResponse student = buildStudentResponse(STUDENT_ID, SCHOOL_ID, PERSON_ID);
        when(studentService.findById(eq(SCHOOL_ID), eq(STUDENT_ID), eq(false))).thenReturn(student);
        when(studentService.isActive(STUDENT_ID)).thenReturn(false);

        service.processSchoolAttendanceEvents(SCHOOL_ID);

        verify(attendanceRepository, never()).save(any());
    }

    // =========================================================================
    // runMarkAbsentJob
    // =========================================================================

    @Test
    void markAbsentJob_shouldCreateAbsentRecord_whenNoRecord() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));
        when(studentService.isActive(STUDENT_ID)).thenReturn(true);
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(eq(STUDENT_ID), any()))
                .thenReturn(false);
        when(attendanceRepository.save(any())).thenAnswer(inv -> {
            DailyAttendanceRecord r = inv.getArgument(0);
            r.setId(200L);
            return r;
        });
        when(settingsService.getBoolean(SCHOOL_ID, "school.attendance.notify.absent", true))
                .thenReturn(false);

        service.runMarkAbsentJob(SCHOOL_ID);

        verify(attendanceRepository).save(argThat(r ->
                r.getStatus() == DailyAttendanceStatus.ABSENT));
        verify(auditService).log(any());
    }

    @Test
    void markAbsentJob_shouldSkip_whenRecordAlreadyExists() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(true);

        SectionEnrollment enrollment = buildEnrollment(STUDENT_ID, SECTION_ID, YEAR_ID);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(enrollment));
        when(studentService.isActive(STUDENT_ID)).thenReturn(true);
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(eq(STUDENT_ID), any()))
                .thenReturn(true);

        service.runMarkAbsentJob(SCHOOL_ID);

        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void markAbsentJob_shouldSkip_whenNotWorkingDay() {
        stubActiveYear();
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(false);

        service.runMarkAbsentJob(SCHOOL_ID);

        verifyNoInteractions(enrollmentRepository, attendanceRepository);
    }

    // =========================================================================
    // overrideAttendance
    // =========================================================================

    @Test
    void overrideAttendance_shouldUpdateStatus_whenValid() {
        DailyAttendanceRecord record = buildRecord(1L, SCHOOL_ID, YEAR_ID,
                SECTION_ID, STUDENT_ID, DailyAttendanceStatus.ABSENT);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(record));

        AcademicYearResponse year = buildYear(YEAR_ID, SCHOOL_ID, AcademicYearStatus.ACTIVE);
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(year);
        when(attendanceRepository.save(any())).thenReturn(record);
        when(attendanceMapper.toResponse(any())).thenReturn(null);

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);
        req.setRemarks("Station malfunction");

        service.overrideAttendance(SCHOOL_ID, 1L, req, 5L);

        assertThat(record.getStatus()).isEqualTo(DailyAttendanceStatus.PRESENT);
        assertThat(record.getMarkedById()).isEqualTo(5L);
        verify(auditService).log(any());
    }

    @Test
    void overrideAttendance_shouldThrow_whenCompletedYear() {
        DailyAttendanceRecord record = buildRecord(1L, SCHOOL_ID, YEAR_ID,
                SECTION_ID, STUDENT_ID, DailyAttendanceStatus.ABSENT);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(record));

        AcademicYearResponse year = buildYear(YEAR_ID, SCHOOL_ID, AcademicYearStatus.COMPLETED);
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(year);

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);

        assertThatThrownBy(() -> service.overrideAttendance(SCHOOL_ID, 1L, req, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void overrideAttendance_shouldThrow404_whenNotFound() {
        when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);

        assertThatThrownBy(() -> service.overrideAttendance(SCHOOL_ID, 99L, req, 5L))
                .isInstanceOf(AttendanceRecordNotFoundException.class);
    }

    @Test
    void overrideAttendance_shouldThrow404_whenBelongsToDifferentSchool() {
        DailyAttendanceRecord record = buildRecord(1L, 99L, YEAR_ID,
                SECTION_ID, STUDENT_ID, DailyAttendanceStatus.ABSENT);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(record));

        OverrideAttendanceRequest req = new OverrideAttendanceRequest();
        req.setStatus(DailyAttendanceStatus.PRESENT);

        assertThatThrownBy(() -> service.overrideAttendance(SCHOOL_ID, 1L, req, 5L))
                .isInstanceOf(AttendanceRecordNotFoundException.class);
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Test
    void setOnLeave_shouldCreateRecord_whenNoneExists() {
        when(attendanceRepository.findByStudentIdAndAttendanceDate(STUDENT_ID, TODAY))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setOnLeave(STUDENT_ID, TODAY, SCHOOL_ID, YEAR_ID, SECTION_ID);

        verify(attendanceRepository).save(argThat(r ->
                r.getStatus() == DailyAttendanceStatus.ON_LEAVE));
    }

    @Test
    void setOnLeave_shouldUpdateExistingRecord_whenAbsent() {
        DailyAttendanceRecord existing = buildRecord(1L, SCHOOL_ID, YEAR_ID,
                SECTION_ID, STUDENT_ID, DailyAttendanceStatus.ABSENT);
        when(attendanceRepository.findByStudentIdAndAttendanceDate(STUDENT_ID, TODAY))
                .thenReturn(Optional.of(existing));
        when(attendanceRepository.save(any())).thenReturn(existing);

        service.setOnLeave(STUDENT_ID, TODAY, SCHOOL_ID, YEAR_ID, SECTION_ID);

        assertThat(existing.getStatus()).isEqualTo(DailyAttendanceStatus.ON_LEAVE);
    }

    @Test
    void resetOnLeave_shouldSetAbsent_whenCurrentlyOnLeave() {
        DailyAttendanceRecord record = buildRecord(1L, SCHOOL_ID, YEAR_ID,
                SECTION_ID, STUDENT_ID, DailyAttendanceStatus.ON_LEAVE);
        when(attendanceRepository.findByStudentIdAndAttendanceDate(STUDENT_ID, TODAY))
                .thenReturn(Optional.of(record));
        when(attendanceRepository.save(any())).thenReturn(record);

        service.resetOnLeave(STUDENT_ID, TODAY, SCHOOL_ID);

        assertThat(record.getStatus()).isEqualTo(DailyAttendanceStatus.ABSENT);
    }

    @Test
    void hasRecord_shouldReturnTrue_whenRecordExists() {
        when(attendanceRepository.existsByStudentIdAndAttendanceDate(STUDENT_ID, TODAY))
                .thenReturn(true);
        assertThat(service.hasRecord(STUDENT_ID, TODAY)).isTrue();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubActiveYear() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID))
                .thenReturn(Optional.of(buildYear(YEAR_ID, SCHOOL_ID, AcademicYearStatus.ACTIVE)));
    }

    private AcademicYearResponse buildYear(Long id, Long schoolId, AcademicYearStatus status) {
        return AcademicYearResponse.builder()
                .id(id).schoolId(schoolId).name("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private SectionEnrollment buildEnrollment(Long studentId, Long sectionId, Long yearId) {
        SectionEnrollment e = SectionEnrollment.builder()
                .studentId(studentId).sectionId(sectionId).academicYearId(yearId)
                .rollNumber("01").enrolledAt(LocalDate.now()).build();
        e.setId(1L);
        return e;
    }

    private StudentResponse buildStudentResponse(Long id, Long schoolId, Long personId) {
        return StudentResponse.builder()
                .id(id).schoolId(schoolId).personId(personId)
                .admissionNumber("ADM-001")
                .status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private AttendanceEventResponse buildEvent(Long id, Long personId, LocalDateTime eventTime) {
        return AttendanceEventResponse.builder()
                .id(id).personId(personId).eventTime(eventTime)
                .direction(EventDirection.ENTRY).source(EventSource.FACE_RECOGNITION)
                .status(AttendanceEventStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();
    }

    private DailyAttendanceRecord buildRecord(Long id, Long schoolId, Long yearId,
                                               Long sectionId, Long studentId,
                                               DailyAttendanceStatus status) {
        DailyAttendanceRecord r = DailyAttendanceRecord.builder()
                .schoolId(schoolId).academicYearId(yearId).sectionId(sectionId)
                .studentId(studentId).attendanceDate(TODAY).status(status).build();
        r.setId(id);
        return r;
    }
}
