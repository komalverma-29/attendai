package com.attendai.school.attendancecorrections.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancecorrections.dto.CorrectionRequestResponse;
import com.attendai.school.attendancecorrections.dto.CorrectionSummaryResponse;
import com.attendai.school.attendancecorrections.dto.CreateCorrectionRequest;
import com.attendai.school.attendancecorrections.dto.ReviewCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.AttendanceCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.exception.CorrectionRequestNotFoundException;
import com.attendai.school.attendancecorrections.mapper.AttendanceCorrectionMapper;
import com.attendai.school.attendancecorrections.repository.AttendanceCorrectionRepository;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.dailyattendance.service.DailyAttendanceService;
import com.attendai.school.settings.service.SchoolSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceCorrectionServiceImpl implements AttendanceCorrectionService {

    private static final String MODULE = "school";

    private final AttendanceCorrectionRepository correctionRepository;
    private final AttendanceCorrectionMapper     correctionMapper;
    private final DailyAttendanceRepository      attendanceRepository;
    private final DailyAttendanceService         attendanceService;
    private final AcademicYearService            academicYearService;
    private final AcademicCalendarService        calendarService;
    private final SchoolSettingsService          settingsService;
    private final AuditService                   auditService;

    @Override
    @Transactional
    public CorrectionRequestResponse submitCorrection(Long schoolId,
                                                       CreateCorrectionRequest request,
                                                       Long requestedById) {
        // BR-CORR-03: ON_LEAVE cannot be set via corrections
        if (DailyAttendanceStatus.ON_LEAVE.equals(request.getRequestedStatus())) {
            throw new ValidationException(
                    "Use school-leave to set ON_LEAVE status — corrections cannot set ON_LEAVE");
        }
        // BR-CORR-01: must be past or current date
        if (request.getAttendanceDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Correction date cannot be in the future");
        }
        // BR-CORR-05: validate academic year not COMPLETED
        var year = academicYearService.getActiveAcademicYearOrThrow(schoolId);
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())) {
            throw new ValidationException("Cannot submit correction for a COMPLETED academic year");
        }
        // BR-CORR-02: must be a working day
        if (!calendarService.isWorkingDay(schoolId, year.getId(), request.getAttendanceDate())) {
            throw new ValidationException(
                    "Correction date " + request.getAttendanceDate() + " is not a working day");
        }
        // Attendance record must exist
        var record = attendanceRepository.findByStudentIdAndAttendanceDate(
                        request.getStudentId(), request.getAttendanceDate())
                .orElseThrow(() -> new ValidationException(
                        "No attendance record found for student " + request.getStudentId()
                        + " on " + request.getAttendanceDate()));
        if (!record.getSchoolId().equals(schoolId)) {
            throw new ValidationException("Attendance record does not belong to school " + schoolId);
        }
        // BR-CORR-04: no duplicate pending
        if (correctionRepository.existsByStudentIdAndAttendanceDateAndStatus(
                request.getStudentId(), request.getAttendanceDate(), CorrectionStatus.PENDING)) {
            throw new ResourceAlreadyExistsException(
                    "A PENDING correction already exists for student " + request.getStudentId()
                    + " on " + request.getAttendanceDate());
        }

        AttendanceCorrectionRequest correction = AttendanceCorrectionRequest.builder()
                .schoolId(schoolId)
                .academicYearId(year.getId())
                .studentId(request.getStudentId())
                .attendanceRecordId(record.getId())
                .attendanceDate(request.getAttendanceDate())
                .originalStatus(record.getStatus())
                .requestedStatus(request.getRequestedStatus())
                .reason(request.getReason())
                .evidenceFileId(request.getEvidenceFileId())
                .status(CorrectionStatus.PENDING)
                .requestedById(requestedById)
                .build();

        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CORRECTION_SUBMITTED").module(MODULE)
                .resourceType("AttendanceCorrectionRequest")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"studentId\":" + request.getStudentId()
                         + ",\"date\":\"" + request.getAttendanceDate() + "\"}")
                .build());

        return correctionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CorrectionRequestResponse findById(Long schoolId, Long id) {
        return correctionMapper.toResponse(require(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CorrectionSummaryResponse> listCorrections(Long schoolId, Long studentId,
                                                            CorrectionStatus status,
                                                            LocalDate fromDate, LocalDate toDate,
                                                            Pageable pageable) {
        return correctionRepository.findByFilters(schoolId, studentId, status,
                        fromDate, toDate, pageable)
                .map(correctionMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public CorrectionRequestResponse approveCorrection(Long schoolId, Long id,
                                                        ReviewCorrectionRequest request,
                                                        Long reviewerId) {
        AttendanceCorrectionRequest correction = require(schoolId, id);
        requirePending(correction, "approve");

        // BR-CORR-06: four-eyes — approver ≠ requester
        if (settingsService.isFourEyesEnabled(schoolId)
                && correction.getRequestedById().equals(reviewerId)) {
            throw new ValidationException(
                    "Approver cannot be the same person as the requester (four-eyes principle)");
        }

        // Apply correction to the attendance record
        OverrideAttendanceRequest override = new OverrideAttendanceRequest();
        override.setStatus(correction.getRequestedStatus());
        if (request != null && request.getRemarks() != null) {
            override.setRemarks(request.getRemarks());
        }
        attendanceService.overrideAttendance(schoolId, correction.getAttendanceRecordId(),
                override, reviewerId);

        correction.setStatus(CorrectionStatus.APPROVED);
        correction.setReviewedById(reviewerId);
        correction.setReviewedAt(LocalDateTime.now());
        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CORRECTION_APPROVED").module(MODULE)
                .resourceType("AttendanceCorrectionRequest")
                .resourceId(String.valueOf(id))
                .details("{\"oldStatus\":\"" + correction.getOriginalStatus()
                         + "\",\"newStatus\":\"" + correction.getRequestedStatus() + "\"}")
                .build());

        return correctionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CorrectionRequestResponse rejectCorrection(Long schoolId, Long id,
                                                       ReviewCorrectionRequest request,
                                                       Long reviewerId) {
        AttendanceCorrectionRequest correction = require(schoolId, id);
        requirePending(correction, "reject");

        correction.setStatus(CorrectionStatus.REJECTED);
        correction.setReviewedById(reviewerId);
        correction.setReviewedAt(LocalDateTime.now());
        if (request != null) correction.setRejectionReason(request.getRejectionReason());
        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CORRECTION_REJECTED").module(MODULE)
                .resourceType("AttendanceCorrectionRequest")
                .resourceId(String.valueOf(id))
                .details("{\"reason\":\"" + correction.getRejectionReason() + "\"}")
                .build());

        return correctionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CorrectionRequestResponse cancelCorrection(Long schoolId, Long id, Long requesterId) {
        AttendanceCorrectionRequest correction = require(schoolId, id);
        requirePending(correction, "cancel");

        correction.setStatus(CorrectionStatus.CANCELLED);
        AttendanceCorrectionRequest saved = correctionRepository.save(correction);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CORRECTION_CANCELLED").module(MODULE)
                .resourceType("AttendanceCorrectionRequest")
                .resourceId(String.valueOf(id)).build());

        return correctionMapper.toResponse(saved);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AttendanceCorrectionRequest require(Long schoolId, Long id) {
        AttendanceCorrectionRequest c = correctionRepository.findById(id)
                .orElseThrow(() -> new CorrectionRequestNotFoundException(id));
        if (!c.getSchoolId().equals(schoolId)) throw new CorrectionRequestNotFoundException(id);
        return c;
    }

    private void requirePending(AttendanceCorrectionRequest c, String action) {
        if (!CorrectionStatus.PENDING.equals(c.getStatus())) {
            throw new ValidationException(
                    "Cannot " + action + " a correction with status: " + c.getStatus());
        }
    }
}
