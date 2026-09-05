package com.attendai.school.dailyattendance.service;

import com.attendai.core.attendance.service.AttendanceService;
import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.notification.dto.SendNotificationRequest;
import com.attendai.core.notification.service.NotificationService;
import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dailyattendance.dto.DailyAttendanceRecordResponse;
import com.attendai.school.dailyattendance.dto.OverrideAttendanceRequest;
import com.attendai.school.dailyattendance.dto.SectionAttendanceSummaryResponse;
import com.attendai.school.dailyattendance.dto.StudentAttendanceDayResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.exception.AttendanceRecordNotFoundException;
import com.attendai.school.dailyattendance.mapper.DailyAttendanceMapper;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.settings.service.SchoolSettingsService;
import com.attendai.school.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAttendanceServiceImpl implements DailyAttendanceService {

    private static final String MODULE   = "school";
    private static final String NOTIF_TYPE = "SCHOOL_STUDENT_ABSENT";

    private final DailyAttendanceRepository    attendanceRepository;
    private final DailyAttendanceMapper        attendanceMapper;
    private final AttendanceService            coreAttendanceService;
    private final AcademicYearService          academicYearService;
    private final AcademicCalendarService      calendarService;
    private final AttendanceRulesService       rulesService;
    private final StudentService               studentService;
    private final SchoolSectionService         sectionService;
    private final SectionEnrollmentRepository  enrollmentRepository;
    private final SchoolSettingsService        settingsService;
    private final NotificationService          notificationService;
    private final AuditService                 auditService;

    // =========================================================================
    // Scheduler entry points
    // =========================================================================

    /**
     * FR-DA-01: For each PENDING core-attendance event for students enrolled in
     * sections of this school, create or update a DailyAttendanceRecord.
     */
    @Override
    @Transactional
    public void processSchoolAttendanceEvents(Long schoolId) {
        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) {
            log.debug("processSchoolAttendanceEvents: no active year for school {}", schoolId);
            return;
        }
        var year  = yearOpt.get();
        LocalDate today = LocalDate.now();

        // Only process on working days
        if (!calendarService.isWorkingDay(schoolId, year.getId(), today)) {
            log.debug("processSchoolAttendanceEvents: {} is not a working day for school {}",
                    today, schoolId);
            return;
        }

        // Get all enrollments for this school's active year
        var enrollments = enrollmentRepository.findByAcademicYearId(year.getId());

        for (var enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            Long sectionId = enrollment.getSectionId();

            // Resolve person id from student
            Long personId;
            try {
                var student = studentService.findById(schoolId, studentId, false);
                personId = student.getPersonId();
            } catch (Exception e) {
                log.warn("Cannot resolve personId for studentId={}: {}", studentId, e.getMessage());
                continue;
            }

            if (!studentService.isActive(studentId)) continue;

            // Get pending core events for this person today
            var events = coreAttendanceService.findPendingEventsForPerson(personId, today);
            if (events.isEmpty()) continue;

            // Idempotency: skip if record already exists
            if (attendanceRepository.existsByStudentIdAndAttendanceDate(studentId, today)) {
                // Still mark core events as processed
                events.forEach(e -> coreAttendanceService.markAsProcessed(e.getId(), MODULE));
                continue;
            }

            // Use earliest ENTRY event
            var entryEvent = events.stream()
                    .filter(e -> e.getEventTime() != null)
                    .min(java.util.Comparator.comparing(e -> e.getEventTime()))
                    .orElse(events.get(0));

            LocalTime arrivalTime = entryEvent.getEventTime().toLocalTime();

            // Apply late threshold rule
            LocalTime lateThreshold;
            try {
                lateThreshold = rulesService.getLateThreshold(sectionId, year.getId());
            } catch (Exception e) {
                lateThreshold = LocalTime.of(9, 0); // fallback default
            }

            DailyAttendanceStatus status = arrivalTime.isAfter(lateThreshold)
                    ? DailyAttendanceStatus.LATE
                    : DailyAttendanceStatus.PRESENT;

            DailyAttendanceRecord record = DailyAttendanceRecord.builder()
                    .schoolId(schoolId)
                    .academicYearId(year.getId())
                    .sectionId(sectionId)
                    .studentId(studentId)
                    .attendanceDate(today)
                    .status(status)
                    .arrivalTime(arrivalTime)
                    .coreEventId(entryEvent.getId())
                    .build();
            attendanceRepository.save(record);

            // Mark all events for this student today as processed
            events.forEach(e -> coreAttendanceService.markAsProcessed(e.getId(), MODULE));

            String auditCode = status == DailyAttendanceStatus.LATE
                    ? "ATTENDANCE_RECORD_LATE" : "ATTENDANCE_RECORD_CREATED";
            auditService.log(AuditEventRequest.builder()
                    .actionCode(auditCode).module(MODULE)
                    .resourceType("DailyAttendanceRecord")
                    .resourceId(String.valueOf(record.getId()))
                    .details("{\"studentId\":" + studentId
                             + ",\"date\":\"" + today
                             + "\",\"status\":\"" + status + "\"}")
                    .build());
        }
    }

    /**
     * FR-DA-02: For every active enrolled student without a record today, create ABSENT.
     */
    @Override
    @Transactional
    public void runMarkAbsentJob(Long schoolId) {
        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) return;
        var year  = yearOpt.get();
        LocalDate today = LocalDate.now();

        if (!calendarService.isWorkingDay(schoolId, year.getId(), today)) {
            log.debug("runMarkAbsentJob: {} is not a working day for school {}", today, schoolId);
            return;
        }

        var enrollments = enrollmentRepository.findByAcademicYearId(year.getId());
        for (var enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            Long sectionId = enrollment.getSectionId();

            if (!studentService.isActive(studentId)) continue;

            // Skip if record already exists (PRESENT, LATE, or ON_LEAVE set by leave module)
            if (attendanceRepository.existsByStudentIdAndAttendanceDate(studentId, today)) continue;

            DailyAttendanceRecord record = DailyAttendanceRecord.builder()
                    .schoolId(schoolId)
                    .academicYearId(year.getId())
                    .sectionId(sectionId)
                    .studentId(studentId)
                    .attendanceDate(today)
                    .status(DailyAttendanceStatus.ABSENT)
                    .build();
            attendanceRepository.save(record);

            auditService.log(AuditEventRequest.builder()
                    .actionCode("ATTENDANCE_RECORD_ABSENT").module(MODULE)
                    .resourceType("DailyAttendanceRecord")
                    .resourceId(String.valueOf(record.getId()))
                    .details("{\"studentId\":" + studentId + ",\"date\":\"" + today + "\"}")
                    .build());

            // FR-DA-06: notify guardian if notifications are enabled
            if (settingsService.getBoolean(schoolId, "school.attendance.notify.absent", true)) {
                try {
                    var student = studentService.findById(schoolId, studentId, false);
                    if (student.getUserId() != null) {
                        notificationService.send(SendNotificationRequest.builder()
                                .recipientUserId(student.getUserId())
                                .typeCode(NOTIF_TYPE)
                                .variables(Map.of("date", today.toString()))
                                .build());
                    }
                } catch (Exception e) {
                    log.warn("Failed to send absent notification for student {}: {}",
                            studentId, e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    // HTTP queries
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SectionAttendanceSummaryResponse getSectionAttendanceForDate(
            Long schoolId, Long sectionId, LocalDate date) {
        // Validate section belongs to this school
        var section = sectionService.findById(sectionId);
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException("Section " + sectionId + " does not belong to school " + schoolId);
        }

        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        boolean isWorkingDay = yearOpt.isPresent()
                && calendarService.isWorkingDay(schoolId, yearOpt.get().getId(), date);

        List<DailyAttendanceRecord> records =
                attendanceRepository.findBySectionIdAndAttendanceDate(sectionId, date);

        // Build per-student rows (just from what's persisted)
        List<StudentAttendanceDayResponse> studentRows = records.stream()
                .map(r -> StudentAttendanceDayResponse.builder()
                        .studentId(r.getStudentId())
                        .rollNumber(null) // enrollment roll number available via enrollmentRepo if needed
                        .status(r.getStatus())
                        .arrivalTime(r.getArrivalTime())
                        .remarks(r.getRemarks())
                        .build())
                .toList();

        // Count by status
        Map<DailyAttendanceStatus, Long> counts = records.stream()
                .collect(Collectors.groupingBy(DailyAttendanceRecord::getStatus, Collectors.counting()));

        return SectionAttendanceSummaryResponse.builder()
                .sectionId(sectionId)
                .date(date)
                .workingDay(isWorkingDay)
                .records(studentRows)
                .present(counts.getOrDefault(DailyAttendanceStatus.PRESENT, 0L).intValue())
                .late(counts.getOrDefault(DailyAttendanceStatus.LATE, 0L).intValue())
                .absent(counts.getOrDefault(DailyAttendanceStatus.ABSENT, 0L).intValue())
                .onLeave(counts.getOrDefault(DailyAttendanceStatus.ON_LEAVE, 0L).intValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAttendanceRecordResponse> getStudentAttendance(
            Long schoolId, Long studentId, LocalDate fromDate, LocalDate toDate,
            Long academicYearId) {
        return attendanceRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        studentId, fromDate, toDate)
                .stream()
                .filter(r -> r.getSchoolId().equals(schoolId))
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DailyAttendanceRecordResponse overrideAttendance(
            Long schoolId, Long recordId, OverrideAttendanceRequest request, Long actorUserId) {
        DailyAttendanceRecord record = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new AttendanceRecordNotFoundException(recordId));
        if (!record.getSchoolId().equals(schoolId)) {
            throw new AttendanceRecordNotFoundException(recordId);
        }

        // BR-DA-08: COMPLETED year is read-only
        var year = academicYearService.findById(schoolId, record.getAcademicYearId());
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())) {
            throw new ValidationException("Cannot modify attendance for a COMPLETED academic year");
        }

        DailyAttendanceStatus oldStatus = record.getStatus();
        record.setStatus(request.getStatus());
        if (request.getRemarks() != null) record.setRemarks(request.getRemarks());
        record.setMarkedById(actorUserId);

        DailyAttendanceRecord saved = attendanceRepository.save(record);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ATTENDANCE_RECORD_OVERRIDDEN").module(MODULE)
                .resourceType("DailyAttendanceRecord")
                .resourceId(String.valueOf(recordId))
                .details("{\"oldStatus\":\"" + oldStatus
                         + "\",\"newStatus\":\"" + request.getStatus() + "\"}")
                .build());

        return attendanceMapper.toResponse(saved);
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Override
    @Transactional
    public void setOnLeave(Long studentId, LocalDate date, Long schoolId,
                            Long academicYearId, Long sectionId) {
        Optional<DailyAttendanceRecord> existing =
                attendanceRepository.findByStudentIdAndAttendanceDate(studentId, date);
        if (existing.isPresent()) {
            DailyAttendanceRecord r = existing.get();
            r.setStatus(DailyAttendanceStatus.ON_LEAVE);
            attendanceRepository.save(r);
        } else {
            DailyAttendanceRecord r = DailyAttendanceRecord.builder()
                    .schoolId(schoolId).academicYearId(academicYearId)
                    .sectionId(sectionId).studentId(studentId)
                    .attendanceDate(date).status(DailyAttendanceStatus.ON_LEAVE)
                    .build();
            attendanceRepository.save(r);
        }
    }

    @Override
    @Transactional
    public void resetOnLeave(Long studentId, LocalDate date, Long schoolId) {
        attendanceRepository.findByStudentIdAndAttendanceDate(studentId, date)
                .ifPresent(r -> {
                    if (DailyAttendanceStatus.ON_LEAVE.equals(r.getStatus())) {
                        // Reset to ABSENT — a correction job or re-processing may update later
                        r.setStatus(DailyAttendanceStatus.ABSENT);
                        attendanceRepository.save(r);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public DailyAttendanceStatus getStatus(Long studentId, LocalDate date) {
        return attendanceRepository.findByStudentIdAndAttendanceDate(studentId, date)
                .map(DailyAttendanceRecord::getStatus)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRecord(Long studentId, LocalDate date) {
        return attendanceRepository.existsByStudentIdAndAttendanceDate(studentId, date);
    }
}
