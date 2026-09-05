package com.attendai.school.attendancereports.service;

import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancereports.dto.AttendanceShortageResponse;
import com.attendai.school.attendancereports.dto.ConsecutiveAbsenceResponse;
import com.attendai.school.attendancereports.dto.DailyAttendanceRegisterResponse;
import com.attendai.school.attendancereports.dto.SchoolAttendanceOverviewResponse;
import com.attendai.school.attendancereports.dto.StudentAttendanceSummaryResponse;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.section.entity.SectionEnrollment;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceReportServiceImpl implements AttendanceReportService {

    private final DailyAttendanceRepository   attendanceRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final AcademicYearService         academicYearService;
    private final AcademicCalendarService     calendarService;
    private final AttendanceRulesService      rulesService;

    // =========================================================================
    // FR-RPT-01: Student attendance summary
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public StudentAttendanceSummaryResponse getStudentSummary(Long schoolId, Long studentId,
                                                               Long academicYearId,
                                                               LocalDate fromDate,
                                                               LocalDate toDate) {
        var year = academicYearService.findById(schoolId, academicYearId);
        LocalDate from = fromDate != null ? fromDate : year.getStartDate();
        LocalDate to   = toDate   != null ? toDate   : year.getEndDate();

        List<DailyAttendanceRecord> records =
                attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        studentId, from, to);
        records = records.stream().filter(r -> r.getSchoolId().equals(schoolId)).toList();

        int present  = count(records, DailyAttendanceStatus.PRESENT);
        int late     = count(records, DailyAttendanceStatus.LATE);
        int absent   = count(records, DailyAttendanceStatus.ABSENT);
        int onLeave  = count(records, DailyAttendanceStatus.ON_LEAVE);

        // BR-RPT-02: ON_LEAVE excluded from denominator
        int workingDays = calendarService.getWorkingDayCount(schoolId, academicYearId, from, to);
        int denominator = workingDays - onLeave;
        if (denominator < 0) denominator = 0;

        BigDecimal pct = denominator > 0
                ? BigDecimal.valueOf(present + late)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal minRequired = rulesService.getMinAttendancePercentage(schoolId, academicYearId);

        return StudentAttendanceSummaryResponse.builder()
                .studentId(studentId).academicYearId(academicYearId)
                .fromDate(from).toDate(to)
                .workingDays(workingDays).presentDays(present).lateDays(late)
                .absentDays(absent).onLeaveDays(onLeave)
                .attendancePercentage(pct).minimumRequired(minRequired)
                .belowThreshold(pct.compareTo(minRequired) < 0)
                .build();
    }

    // =========================================================================
    // FR-RPT-02: Section attendance summary
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceSummaryResponse> getSectionSummary(Long schoolId, Long sectionId,
                                                                     Long academicYearId,
                                                                     LocalDate fromDate,
                                                                     LocalDate toDate) {
        var enrollments = enrollmentRepository.findBySectionIdOrderByRollNumberAsc(sectionId);
        List<StudentAttendanceSummaryResponse> results = new ArrayList<>();
        for (SectionEnrollment enrollment : enrollments) {
            results.add(getStudentSummary(schoolId, enrollment.getStudentId(),
                    academicYearId, fromDate, toDate));
        }
        return results;
    }

    // =========================================================================
    // FR-RPT-03: Shortage report
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceShortageResponse> getShortageReport(Long schoolId, Long academicYearId,
                                                               Long sectionId) {
        var year = academicYearService.findById(schoolId, academicYearId);
        List<SectionEnrollment> enrollments = sectionId != null
                ? enrollmentRepository.findBySectionIdOrderByRollNumberAsc(sectionId)
                : enrollmentRepository.findByAcademicYearId(academicYearId);

        BigDecimal threshold = rulesService.getMinAttendancePercentage(schoolId, academicYearId);
        LocalDate from = year.getStartDate();
        LocalDate to   = LocalDate.now().isAfter(year.getEndDate()) ? year.getEndDate() : LocalDate.now();

        List<AttendanceShortageResponse> shortages = new ArrayList<>();
        for (SectionEnrollment e : enrollments) {
            StudentAttendanceSummaryResponse summary =
                    getStudentSummary(schoolId, e.getStudentId(), academicYearId, from, to);
            if (summary.isBelowThreshold()) {
                BigDecimal shortfall = threshold.subtract(summary.getAttendancePercentage())
                        .setScale(2, RoundingMode.HALF_UP);
                shortages.add(AttendanceShortageResponse.builder()
                        .studentId(e.getStudentId())
                        .sectionId(e.getSectionId())
                        .attendancePercentage(summary.getAttendancePercentage())
                        .minimumRequired(threshold)
                        .shortfallPercentage(shortfall)
                        .build());
            }
        }
        return shortages;
    }

    // =========================================================================
    // FR-RPT-04: Daily register
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public DailyAttendanceRegisterResponse getDailyRegister(Long schoolId, Long sectionId,
                                                             Long academicYearId,
                                                             LocalDate fromDate,
                                                             LocalDate toDate) {
        List<LocalDate> workingDates =
                calendarService.getWorkingDates(schoolId, academicYearId, fromDate, toDate);

        List<DailyAttendanceRecord> allRecords =
                attendanceRepository.findBySectionIdAndAttendanceDateBetween(
                        sectionId, fromDate, toDate);

        // Map: studentId → (date → record)
        Map<Long, Map<LocalDate, DailyAttendanceRecord>> byStudent = allRecords.stream()
                .collect(Collectors.groupingBy(DailyAttendanceRecord::getStudentId,
                        Collectors.toMap(DailyAttendanceRecord::getAttendanceDate, r -> r)));

        List<SectionEnrollment> enrollments =
                enrollmentRepository.findBySectionIdOrderByRollNumberAsc(sectionId);

        List<DailyAttendanceRegisterResponse.StudentRegisterRow> rows = new ArrayList<>();
        for (SectionEnrollment enrollment : enrollments) {
            Map<LocalDate, DailyAttendanceRecord> studentRecords =
                    byStudent.getOrDefault(enrollment.getStudentId(), Map.of());
            List<String> cells = workingDates.stream()
                    .map(d -> {
                        DailyAttendanceRecord r = studentRecords.get(d);
                        if (r == null) return "-";
                        return switch (r.getStatus()) {
                            case PRESENT  -> "P";
                            case LATE     -> "L";
                            case ABSENT   -> "A";
                            case ON_LEAVE -> "OL";
                        };
                    })
                    .toList();
            rows.add(DailyAttendanceRegisterResponse.StudentRegisterRow.builder()
                    .studentId(enrollment.getStudentId())
                    .rollNumber(enrollment.getRollNumber())
                    .attendance(cells)
                    .build());
        }

        return DailyAttendanceRegisterResponse.builder()
                .sectionId(sectionId).workingDates(workingDates).students(rows).build();
    }

    // =========================================================================
    // FR-RPT-05: Consecutive absences
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ConsecutiveAbsenceResponse> getConsecutiveAbsences(Long schoolId,
                                                                     Long academicYearId,
                                                                     Long sectionId,
                                                                     int minDays) {
        var year = academicYearService.findById(schoolId, academicYearId);
        LocalDate from = year.getStartDate();
        LocalDate to   = LocalDate.now().isAfter(year.getEndDate()) ? year.getEndDate() : LocalDate.now();

        List<LocalDate> workingDates = calendarService.getWorkingDates(schoolId, academicYearId, from, to);

        List<SectionEnrollment> enrollments = sectionId != null
                ? enrollmentRepository.findBySectionIdOrderByRollNumberAsc(sectionId)
                : enrollmentRepository.findByAcademicYearId(academicYearId);

        List<ConsecutiveAbsenceResponse> result = new ArrayList<>();
        for (SectionEnrollment e : enrollments) {
            List<DailyAttendanceRecord> records =
                    attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                            e.getStudentId(), from, to);
            Map<LocalDate, DailyAttendanceStatus> statusMap = records.stream()
                    .collect(Collectors.toMap(DailyAttendanceRecord::getAttendanceDate,
                            DailyAttendanceRecord::getStatus));

            int streak = 0;
            LocalDate streakStart = null;
            for (LocalDate d : workingDates) {
                DailyAttendanceStatus s = statusMap.get(d);
                if (DailyAttendanceStatus.ABSENT.equals(s)) {
                    streak++;
                    if (streakStart == null) streakStart = d;
                } else {
                    if (streak >= minDays) {
                        result.add(ConsecutiveAbsenceResponse.builder()
                                .studentId(e.getStudentId()).sectionId(e.getSectionId())
                                .streakStartDate(streakStart).consecutiveDays(streak).build());
                    }
                    streak = 0; streakStart = null;
                }
            }
            if (streak >= minDays) {
                result.add(ConsecutiveAbsenceResponse.builder()
                        .studentId(e.getStudentId()).sectionId(e.getSectionId())
                        .streakStartDate(streakStart).consecutiveDays(streak).build());
            }
        }
        return result;
    }

    // =========================================================================
    // FR-RPT-06: School overview
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SchoolAttendanceOverviewResponse getSchoolOverview(Long schoolId,
                                                               LocalDate fromDate,
                                                               LocalDate toDate) {
        List<DailyAttendanceRecord> records =
                attendanceRepository.findBySchoolIdAndDateRange(schoolId, fromDate, toDate);

        long totalStudents = records.stream().map(DailyAttendanceRecord::getStudentId).distinct().count();
        int present = count(records, DailyAttendanceStatus.PRESENT);
        int late    = count(records, DailyAttendanceStatus.LATE);
        int absent  = count(records, DailyAttendanceStatus.ABSENT);
        int onLeave = count(records, DailyAttendanceStatus.ON_LEAVE);

        int denominator = present + late + absent; // exclude ON_LEAVE per BR-RPT-02
        BigDecimal pct = denominator > 0
                ? BigDecimal.valueOf(present + late)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return SchoolAttendanceOverviewResponse.builder()
                .schoolId(schoolId).fromDate(fromDate).toDate(toDate)
                .totalStudents((int) totalStudents)
                .presentCount(present).lateCount(late).absentCount(absent).onLeaveCount(onLeave)
                .attendancePercentage(pct)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private int count(List<DailyAttendanceRecord> records, DailyAttendanceStatus status) {
        return (int) records.stream().filter(r -> status.equals(r.getStatus())).count();
    }
}
