package com.attendai.school.leave.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.leave.dto.CreateLeaveApplicationRequest;
import com.attendai.school.leave.dto.LeaveApplicationResponse;
import com.attendai.school.leave.dto.LeaveApplicationSummaryResponse;
import com.attendai.school.leave.dto.ReviewLeaveRequest;
import com.attendai.school.leave.entity.LeaveApplication;
import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import com.attendai.school.leave.exception.LeaveApplicationNotFoundException;
import com.attendai.school.leave.mapper.LeaveApplicationMapper;
import com.attendai.school.leave.repository.LeaveApplicationRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.student.service.StudentService;
import com.attendai.school.teacher.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    private static final String MODULE = "school";

    private final LeaveApplicationRepository leaveRepository;
    private final LeaveApplicationMapper     leaveMapper;
    private final AcademicYearService        academicYearService;
    private final AcademicCalendarService    calendarService;
    private final StudentService             studentService;
    private final TeacherService             teacherService;
    private final DailyAttendanceService     attendanceService;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final AuditService               auditService;

    @Override
    @Transactional
    public LeaveApplicationResponse createLeave(Long schoolId,
                                                 CreateLeaveApplicationRequest request) {
        // Validate academic year
        Long academicYearId = resolveAcademicYearId(schoolId, request.getAcademicYearId());
        var year = academicYearService.findById(schoolId, academicYearId);
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())
                || AcademicYearStatus.CANCELLED.equals(year.getStatus())) {
            throw new ValidationException(
                    "Cannot create leave for a " + year.getStatus() + " academic year");
        }

        // BR-LEAVE-02: end date >= start date
        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();
        if (end.isBefore(start)) {
            throw new ValidationException("End date must be on or after start date");
        }

        // BR-LEAVE-01: start date cannot be more than 7 days in the past
        if (start.isBefore(LocalDate.now().minusDays(7))) {
            throw new ValidationException(
                    "Leave start date cannot be more than 7 days in the past");
        }

        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;

        // Validate applicant and check overlaps
        if (LeaveApplicantType.STUDENT.equals(request.getApplicantType())) {
            if (request.getStudentId() == null) {
                throw new ValidationException("studentId is required for STUDENT leave");
            }
            studentService.findById(schoolId, request.getStudentId()); // validates school scope
            // BR-LEAVE-04: no overlapping pending/approved leave
            if (leaveRepository.existsOverlappingStudentLeave(
                    request.getStudentId(), start, end)) {
                throw new ResourceAlreadyExistsException(
                        "Student already has a PENDING or APPROVED leave overlapping this period");
            }
        } else {
            if (request.getTeacherId() == null) {
                throw new ValidationException("teacherId is required for TEACHER leave");
            }
            teacherService.findById(schoolId, request.getTeacherId()); // validates school scope
            if (leaveRepository.existsOverlappingTeacherLeave(
                    request.getTeacherId(), start, end)) {
                throw new ResourceAlreadyExistsException(
                        "Teacher already has a PENDING or APPROVED leave overlapping this period");
            }
        }

        LeaveApplication leave = LeaveApplication.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .applicantType(request.getApplicantType())
                .studentId(request.getStudentId())
                .teacherId(request.getTeacherId())
                .leaveType(request.getLeaveType())
                .startDate(start)
                .endDate(end)
                .totalDays(totalDays)
                .reason(request.getReason())
                .evidenceFileId(request.getEvidenceFileId())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveApplication saved = leaveRepository.save(leave);

        auditService.log(AuditEventRequest.builder()
                .actionCode("LEAVE_CREATED").module(MODULE)
                .resourceType("LeaveApplication")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"type\":\"" + request.getLeaveType() + "\"}")
                .build());

        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveApplicationResponse findById(Long schoolId, Long id) {
        return leaveMapper.toResponse(requireLeave(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveApplicationSummaryResponse> listLeaves(Long schoolId, Long studentId,
                                                             Long teacherId, LeaveStatus status,
                                                             LeaveType leaveType,
                                                             LocalDate fromDate, LocalDate toDate,
                                                             Pageable pageable) {
        return leaveRepository.findByFilters(schoolId, studentId, teacherId, status,
                        leaveType, fromDate, toDate, pageable)
                .map(leaveMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public LeaveApplicationResponse approveLeave(Long schoolId, Long id,
                                                  ReviewLeaveRequest request, Long actorUserId) {
        LeaveApplication leave = requireLeave(schoolId, id);
        requireStatus(leave, LeaveStatus.PENDING, "approve");

        // BR-LEAVE-05: cannot approve for COMPLETED year
        var year = academicYearService.findById(schoolId, leave.getAcademicYearId());
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())) {
            throw new ValidationException("Cannot approve leave for a COMPLETED academic year");
        }

        // Compute working days and set ON_LEAVE attendance records
        // BR-LEAVE-03: only working days within the range
        List<LocalDate> workingDates = calendarService.getWorkingDates(
                schoolId, leave.getAcademicYearId(), leave.getStartDate(), leave.getEndDate());

        leave.setWorkingDays(workingDates.size());
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApprovedById(actorUserId);
        leave.setApprovedAt(LocalDateTime.now());

        LeaveApplication saved = leaveRepository.save(leave);

        // FR-LEAVE-04: set ON_LEAVE on DailyAttendanceRecord for each working day
        if (LeaveApplicantType.STUDENT.equals(leave.getApplicantType())
                && leave.getStudentId() != null) {
            Long studentId = leave.getStudentId();
            // Find student's section for this year
            var enrollmentOpt = enrollmentRepository
                    .findByStudentIdAndAcademicYearId(studentId, leave.getAcademicYearId());
            Long sectionId = enrollmentOpt.map(e -> e.getSectionId()).orElse(null);

            for (LocalDate workingDate : workingDates) {
                try {
                    attendanceService.setOnLeave(studentId, workingDate, schoolId,
                            leave.getAcademicYearId(), sectionId != null ? sectionId : 0L);
                } catch (Exception e) {
                    log.warn("Failed to set ON_LEAVE for student {} on {}: {}",
                            studentId, workingDate, e.getMessage());
                }
            }
        }

        auditService.log(AuditEventRequest.builder()
                .actionCode("LEAVE_APPROVED").module(MODULE)
                .resourceType("LeaveApplication")
                .resourceId(String.valueOf(id))
                .details("{\"approvedById\":" + actorUserId + "}")
                .build());

        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LeaveApplicationResponse rejectLeave(Long schoolId, Long id,
                                                 ReviewLeaveRequest request, Long actorUserId) {
        LeaveApplication leave = requireLeave(schoolId, id);
        requireStatus(leave, LeaveStatus.PENDING, "reject");

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(request != null ? request.getRejectionReason() : null);
        LeaveApplication saved = leaveRepository.save(leave);

        auditService.log(AuditEventRequest.builder()
                .actionCode("LEAVE_REJECTED").module(MODULE)
                .resourceType("LeaveApplication").resourceId(String.valueOf(id)).build());

        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LeaveApplicationResponse cancelLeave(Long schoolId, Long id, Long actorUserId) {
        LeaveApplication leave = requireLeave(schoolId, id);
        requireStatus(leave, LeaveStatus.PENDING, "cancel");

        leave.setStatus(LeaveStatus.CANCELLED);
        LeaveApplication saved = leaveRepository.save(leave);

        auditService.log(AuditEventRequest.builder()
                .actionCode("LEAVE_CANCELLED").module(MODULE)
                .resourceType("LeaveApplication").resourceId(String.valueOf(id)).build());

        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LeaveApplicationResponse revokeLeave(Long schoolId, Long id, Long actorUserId) {
        LeaveApplication leave = requireLeave(schoolId, id);
        requireStatus(leave, LeaveStatus.APPROVED, "revoke");

        leave.setStatus(LeaveStatus.REVOKED);
        leave.setRevokedById(actorUserId);
        leave.setRevokedAt(LocalDateTime.now());
        LeaveApplication saved = leaveRepository.save(leave);

        // FR-LEAVE-07: reset ON_LEAVE attendance records for remaining days
        if (LeaveApplicantType.STUDENT.equals(leave.getApplicantType())
                && leave.getStudentId() != null) {
            LocalDate today = LocalDate.now();
            LocalDate from = leave.getStartDate().isAfter(today) ? leave.getStartDate() : today;
            List<LocalDate> workingDates = calendarService.getWorkingDates(
                    schoolId, leave.getAcademicYearId(), from, leave.getEndDate());
            for (LocalDate date : workingDates) {
                try {
                    attendanceService.resetOnLeave(leave.getStudentId(), date, schoolId);
                } catch (Exception e) {
                    log.warn("Failed to reset ON_LEAVE for student {} on {}: {}",
                            leave.getStudentId(), date, e.getMessage());
                }
            }
        }

        auditService.log(AuditEventRequest.builder()
                .actionCode("LEAVE_REVOKED").module(MODULE)
                .resourceType("LeaveApplication")
                .resourceId(String.valueOf(id))
                .details("{\"revokedById\":" + actorUserId + "}")
                .build());

        return leaveMapper.toResponse(saved);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private LeaveApplication requireLeave(Long schoolId, Long id) {
        LeaveApplication l = leaveRepository.findById(id)
                .orElseThrow(() -> new LeaveApplicationNotFoundException(id));
        if (!l.getSchoolId().equals(schoolId))
            throw new LeaveApplicationNotFoundException(id);
        return l;
    }

    private void requireStatus(LeaveApplication leave, LeaveStatus required, String action) {
        if (!required.equals(leave.getStatus())) {
            throw new ValidationException(
                    "Cannot " + action + " a leave application with status: "
                    + leave.getStatus());
        }
    }

    private Long resolveAcademicYearId(Long schoolId, Long requestedYearId) {
        if (requestedYearId != null) return requestedYearId;
        return academicYearService.getActiveAcademicYearOrThrow(schoolId).getId();
    }
}
