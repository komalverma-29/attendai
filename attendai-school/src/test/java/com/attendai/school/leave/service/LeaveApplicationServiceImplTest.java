package com.attendai.school.leave.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.leave.dto.CreateLeaveApplicationRequest;
import com.attendai.school.leave.dto.ReviewLeaveRequest;
import com.attendai.school.leave.entity.LeaveApplication;
import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import com.attendai.school.leave.exception.LeaveApplicationNotFoundException;
import com.attendai.school.leave.mapper.LeaveApplicationMapper;
import com.attendai.school.leave.repository.LeaveApplicationRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.entity.StudentStatus;
import com.attendai.school.student.service.StudentService;
import com.attendai.school.teacher.service.TeacherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApplicationServiceImplTest {

    @Mock LeaveApplicationRepository  leaveRepository;
    @Mock LeaveApplicationMapper      leaveMapper;
    @Mock AcademicYearService         academicYearService;
    @Mock AcademicCalendarService     calendarService;
    @Mock StudentService              studentService;
    @Mock TeacherService              teacherService;
    @Mock DailyAttendanceService      attendanceService;
    @Mock SectionEnrollmentRepository enrollmentRepository;
    @Mock AuditService                auditService;

    private LeaveApplicationServiceImpl service;

    private static final Long SCHOOL_ID = 1L;
    private static final Long YEAR_ID   = 10L;
    private static final Long STUDENT_ID = 30L;

    @BeforeEach
    void setUp() {
        service = new LeaveApplicationServiceImpl(
                leaveRepository, leaveMapper, academicYearService, calendarService,
                studentService, teacherService, attendanceService, enrollmentRepository,
                auditService);
    }

    // =========================================================================
    // createLeave
    // =========================================================================

    @Test
    void createLeave_shouldSave_whenValidStudentRequest() {
        stubActiveYear();
        when(studentService.findById(SCHOOL_ID, STUDENT_ID)).thenReturn(buildStudent());
        when(leaveRepository.existsOverlappingStudentLeave(
                eq(STUDENT_ID), any(), any())).thenReturn(false);

        LeaveApplication saved = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.save(any())).thenReturn(saved);
        when(leaveMapper.toResponse(saved)).thenReturn(null);

        service.createLeave(SCHOOL_ID, buildCreateRequest(STUDENT_ID, null,
                LocalDate.now(), LocalDate.now().plusDays(2)));

        verify(leaveRepository).save(any(LeaveApplication.class));
        verify(auditService).log(any());
    }

    @Test
    void createLeave_shouldThrow_whenEndDateBeforeStartDate() {
        stubActiveYear();

        assertThatThrownBy(() -> service.createLeave(SCHOOL_ID,
                buildCreateRequest(STUDENT_ID, null,
                        LocalDate.now().plusDays(3), LocalDate.now())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("End date");
    }

    @Test
    void createLeave_shouldThrow_whenStartDateMoreThan7DaysInPast() {
        stubActiveYear();

        assertThatThrownBy(() -> service.createLeave(SCHOOL_ID,
                buildCreateRequest(STUDENT_ID, null,
                        LocalDate.now().minusDays(10), LocalDate.now())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("7 days");
    }

    @Test
    void createLeave_shouldThrow_whenCompletedYear() {
        stubYearWithStatus(AcademicYearStatus.COMPLETED);

        assertThatThrownBy(() -> service.createLeave(SCHOOL_ID,
                buildCreateRequest(STUDENT_ID, null,
                        LocalDate.now(), LocalDate.now().plusDays(1))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void createLeave_shouldThrow409_whenOverlappingLeave() {
        stubActiveYear();
        when(studentService.findById(SCHOOL_ID, STUDENT_ID)).thenReturn(buildStudent());
        when(leaveRepository.existsOverlappingStudentLeave(
                eq(STUDENT_ID), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createLeave(SCHOOL_ID,
                buildCreateRequest(STUDENT_ID, null,
                        LocalDate.now(), LocalDate.now().plusDays(1))))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createLeave_shouldThrow_whenStudentIdMissingForStudentLeave() {
        stubActiveYear();

        CreateLeaveApplicationRequest req = buildCreateRequest(null, null,
                LocalDate.now(), LocalDate.now().plusDays(1));
        req.setApplicantType(LeaveApplicantType.STUDENT);

        assertThatThrownBy(() -> service.createLeave(SCHOOL_ID, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("studentId");
    }

    // =========================================================================
    // approveLeave
    // =========================================================================

    @Test
    void approveLeave_shouldApproveAndSetOnLeave_whenValid() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));

        stubActiveYear();
        LocalDate start = LocalDate.now();
        LocalDate end   = start.plusDays(2);
        leave.setStartDate(start);
        leave.setEndDate(end);

        when(calendarService.getWorkingDates(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(List.of(start, start.plusDays(1), end));
        when(enrollmentRepository.findByStudentIdAndAcademicYearId(STUDENT_ID, YEAR_ID))
                .thenReturn(Optional.empty());
        when(leaveRepository.save(any())).thenReturn(leave);
        when(leaveMapper.toResponse(any())).thenReturn(null);

        service.approveLeave(SCHOOL_ID, 1L, new ReviewLeaveRequest(), 5L);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leave.getApprovedById()).isEqualTo(5L);
        verify(attendanceService, times(3)).setOnLeave(
                eq(STUDENT_ID), any(), eq(SCHOOL_ID), eq(YEAR_ID), any());
        verify(auditService).log(any());
    }

    @Test
    void approveLeave_shouldThrow_whenNotPending() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.REJECTED);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.approveLeave(SCHOOL_ID, 1L, null, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void approveLeave_shouldThrow_whenCompletedYear() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        stubYearWithStatus(AcademicYearStatus.COMPLETED);

        assertThatThrownBy(() -> service.approveLeave(SCHOOL_ID, 1L, null, 5L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
    }

    // =========================================================================
    // rejectLeave
    // =========================================================================

    @Test
    void rejectLeave_shouldReject_whenPending() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any())).thenReturn(leave);
        when(leaveMapper.toResponse(any())).thenReturn(null);

        ReviewLeaveRequest req = new ReviewLeaveRequest();
        req.setRejectionReason("Insufficient balance");
        service.rejectLeave(SCHOOL_ID, 1L, req, 5L);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(leave.getRejectionReason()).isEqualTo("Insufficient balance");
    }

    // =========================================================================
    // cancelLeave
    // =========================================================================

    @Test
    void cancelLeave_shouldCancel_whenPending() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any())).thenReturn(leave);
        when(leaveMapper.toResponse(any())).thenReturn(null);

        service.cancelLeave(SCHOOL_ID, 1L, 30L);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
    }

    @Test
    void cancelLeave_shouldThrow_whenAlreadyApproved() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.APPROVED);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.cancelLeave(SCHOOL_ID, 1L, 30L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("APPROVED");
    }

    // =========================================================================
    // revokeLeave
    // =========================================================================

    @Test
    void revokeLeave_shouldRevoke_whenApproved() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.APPROVED);
        leave.setStartDate(LocalDate.now().plusDays(1));
        leave.setEndDate(LocalDate.now().plusDays(3));
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));
        when(calendarService.getWorkingDates(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(List.of(leave.getStartDate()));
        when(leaveRepository.save(any())).thenReturn(leave);
        when(leaveMapper.toResponse(any())).thenReturn(null);

        service.revokeLeave(SCHOOL_ID, 1L, 5L);

        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.REVOKED);
        assertThat(leave.getRevokedById()).isEqualTo(5L);
        verify(attendanceService).resetOnLeave(eq(STUDENT_ID), any(), eq(SCHOOL_ID));
    }

    @Test
    void revokeLeave_shouldThrow_whenNotApproved() {
        LeaveApplication leave = buildLeave(1L, SCHOOL_ID, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.revokeLeave(SCHOOL_ID, 1L, 5L))
                .isInstanceOf(ValidationException.class);
    }

    // =========================================================================
    // findById
    // =========================================================================

    @Test
    void findById_shouldThrow404_whenBelongsToDifferentSchool() {
        LeaveApplication leave = buildLeave(1L, 99L, YEAR_ID,
                LeaveApplicantType.STUDENT, STUDENT_ID, null, LeaveStatus.PENDING);
        when(leaveRepository.findById(1L)).thenReturn(Optional.of(leave));

        assertThatThrownBy(() -> service.findById(SCHOOL_ID, 1L))
                .isInstanceOf(LeaveApplicationNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(leaveRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(SCHOOL_ID, 99L))
                .isInstanceOf(LeaveApplicationNotFoundException.class);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubActiveYear() { stubYearWithStatus(AcademicYearStatus.ACTIVE); }

    private void stubYearWithStatus(AcademicYearStatus status) {
        AcademicYearResponse year = AcademicYearResponse.builder()
                .id(YEAR_ID).schoolId(SCHOOL_ID).name("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(year);
    }

    private StudentResponse buildStudent() {
        return StudentResponse.builder()
                .id(STUDENT_ID).schoolId(SCHOOL_ID).personId(99L)
                .admissionNumber("ADM-001").status(StudentStatus.ACTIVE)
                .enrollmentDate(LocalDate.now())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private LeaveApplication buildLeave(Long id, Long schoolId, Long yearId,
                                         LeaveApplicantType type, Long studentId,
                                         Long teacherId, LeaveStatus status) {
        LeaveApplication l = LeaveApplication.builder()
                .schoolId(schoolId).academicYearId(yearId).applicantType(type)
                .studentId(studentId).teacherId(teacherId)
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1))
                .totalDays(2).reason("Sick").status(status).build();
        l.setId(id);
        return l;
    }

    private CreateLeaveApplicationRequest buildCreateRequest(Long studentId, Long teacherId,
                                                              LocalDate start, LocalDate end) {
        CreateLeaveApplicationRequest req = new CreateLeaveApplicationRequest();
        req.setApplicantType(studentId != null ? LeaveApplicantType.STUDENT : LeaveApplicantType.TEACHER);
        req.setStudentId(studentId);
        req.setTeacherId(teacherId);
        req.setLeaveType(LeaveType.SICK);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setReason("Sick");
        req.setAcademicYearId(YEAR_ID);
        return req;
    }
}
