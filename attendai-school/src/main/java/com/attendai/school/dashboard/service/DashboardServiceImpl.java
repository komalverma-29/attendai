package com.attendai.school.dashboard.service;

import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.repository.AttendanceCorrectionRepository;
import com.attendai.school.attendancereports.dto.ConsecutiveAbsenceResponse;
import com.attendai.school.attendancereports.service.AttendanceReportService;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dashboard.dto.AttendanceTrendResponse;
import com.attendai.school.dashboard.dto.LowAttendanceAlertResponse;
import com.attendai.school.dashboard.dto.PendingActionsResponse;
import com.attendai.school.dashboard.dto.SchoolOverviewResponse;
import com.attendai.school.dashboard.dto.SectionDailySummaryResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.repository.LeaveApplicationRepository;
import com.attendai.school.section.entity.SectionEnrollment;
import com.attendai.school.section.repository.SchoolSectionRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int TREND_DAYS = 30;

    private final DailyAttendanceRepository      attendanceRepository;
    private final SchoolSectionRepository        sectionRepository;
    private final SectionEnrollmentRepository    enrollmentRepository;
    private final AcademicYearService            academicYearService;
    private final AcademicCalendarService        calendarService;
    private final AttendanceRulesService         rulesService;
    private final AttendanceReportService        reportService;
    private final LeaveApplicationRepository     leaveRepository;
    private final AttendanceCorrectionRepository correctionRepository;

    // =========================================================================
    // FR-DASH-01: School overview for today
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SchoolOverviewResponse getSchoolOverview(Long schoolId) {
        LocalDate today = LocalDate.now();

        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) {
            return emptyOverview(schoolId, today, false);
        }
        Long yearId = yearOpt.get().getId();

        boolean isWorkingDay = calendarService.isWorkingDay(schoolId, yearId, today);
        if (!isWorkingDay) {
            // BR-DASH-01: return zeros on non-working day
            return emptyOverview(schoolId, today, false);
        }

        List<DailyAttendanceRecord> todayRecords =
                attendanceRepository.findBySchoolIdAndDateRange(schoolId, today, today);

        int present = count(todayRecords, DailyAttendanceStatus.PRESENT);
        int late    = count(todayRecords, DailyAttendanceStatus.LATE);
        int absent  = count(todayRecords, DailyAttendanceStatus.ABSENT);
        int onLeave = count(todayRecords, DailyAttendanceStatus.ON_LEAVE);
        int total   = todayRecords.stream()
                .map(DailyAttendanceRecord::getStudentId).collect(Collectors.toSet()).size();

        int denominator = present + late + absent;
        BigDecimal pct = denominator > 0
                ? pct(present + late, denominator)
                : BigDecimal.ZERO;

        // FR-DASH-05: pending action counts
        long pendingCorrections = correctionRepository.countBySchoolIdAndStatus(
                schoolId, CorrectionStatus.PENDING);
        long pendingLeaves = leaveRepository.countBySchoolIdAndStatus(
                schoolId, LeaveStatus.PENDING);

        // Consecutive absence alerts using rules default
        int consecutiveThreshold;
        try {
            consecutiveThreshold = (int) rulesService.getConsecutiveAbsenceAlert(0L, yearId);
        } catch (Exception e) {
            consecutiveThreshold = 3;
        }
        long consecutiveAlerts = reportService
                .getConsecutiveAbsences(schoolId, yearId, null, consecutiveThreshold)
                .size();

        PendingActionsResponse pendingActions = PendingActionsResponse.builder()
                .correctionRequests(pendingCorrections)
                .leaveApplications(pendingLeaves)
                .consecutiveAbsenceAlerts(consecutiveAlerts)
                .build();

        return SchoolOverviewResponse.builder()
                .date(today).workingDay(true).academicYearId(yearId)
                .totalStudents(total)
                .present(present).late(late).absent(absent).onLeave(onLeave)
                .attendancePercentage(pct)
                .pendingActions(pendingActions)
                .build();
    }

    // =========================================================================
    // FR-DASH-02: Section-wise summary for today
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SectionDailySummaryResponse> getSectionsSummaryToday(Long schoolId) {
        LocalDate today = LocalDate.now();
        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) return List.of();

        Long yearId = yearOpt.get().getId();
        if (!calendarService.isWorkingDay(schoolId, yearId, today)) return List.of();

        var sections = sectionRepository.findBySchoolIdAndAcademicYearId(schoolId, yearId);
        List<SectionDailySummaryResponse> result = new ArrayList<>();

        for (var section : sections) {
            List<DailyAttendanceRecord> records =
                    attendanceRepository.findBySectionIdAndAttendanceDate(section.getId(), today);
            int enrolled = (int) enrollmentRepository.countBySectionId(section.getId());

            int present = count(records, DailyAttendanceStatus.PRESENT);
            int late    = count(records, DailyAttendanceStatus.LATE);
            int absent  = count(records, DailyAttendanceStatus.ABSENT);
            int onLeave = count(records, DailyAttendanceStatus.ON_LEAVE);

            int denominator = present + late + absent;
            BigDecimal sectionPct = denominator > 0 ? pct(present + late, denominator) : BigDecimal.ZERO;

            result.add(SectionDailySummaryResponse.builder()
                    .sectionId(section.getId())
                    .sectionName(section.getName())
                    .totalStudents(enrolled)
                    .present(present).late(late).absent(absent).onLeave(onLeave)
                    .attendancePercentage(sectionPct)
                    .build());
        }
        return result;
    }

    // =========================================================================
    // FR-DASH-03: Attendance trend (last 30 working days)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public AttendanceTrendResponse getAttendanceTrend(Long schoolId, Long sectionId) {
        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) {
            return AttendanceTrendResponse.builder().sectionId(sectionId).trend(List.of()).build();
        }
        Long yearId = yearOpt.get().getId();
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusDays(90); // look back up to 90 calendar days to find 30 working

        List<LocalDate> workingDates = calendarService.getWorkingDates(schoolId, yearId, from, today);
        // Take only last 30 working days
        if (workingDates.size() > TREND_DAYS) {
            workingDates = workingDates.subList(workingDates.size() - TREND_DAYS, workingDates.size());
        }

        List<AttendanceTrendResponse.TrendPoint> points = new ArrayList<>();
        for (LocalDate date : workingDates) {
            List<DailyAttendanceRecord> records = sectionId != null
                    ? attendanceRepository.findBySectionIdAndAttendanceDate(sectionId, date)
                    : attendanceRepository.findBySchoolIdAndDateRange(schoolId, date, date);

            int present = count(records, DailyAttendanceStatus.PRESENT);
            int late    = count(records, DailyAttendanceStatus.LATE);
            int absent  = count(records, DailyAttendanceStatus.ABSENT);
            int denom   = present + late + absent;
            BigDecimal dayPct = denom > 0 ? pct(present + late, denom) : BigDecimal.ZERO;
            points.add(AttendanceTrendResponse.TrendPoint.builder()
                    .date(date).percentage(dayPct).build());
        }
        return AttendanceTrendResponse.builder().sectionId(sectionId).trend(points).build();
    }

    // =========================================================================
    // FR-DASH-04: Low attendance alerts
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<LowAttendanceAlertResponse> getLowAttendanceAlerts(Long schoolId, Long sectionId) {
        var yearOpt = academicYearService.getActiveAcademicYear(schoolId);
        if (yearOpt.isEmpty()) return List.of();
        Long yearId = yearOpt.get().getId();

        BigDecimal threshold = rulesService.getMinAttendancePercentage(schoolId, yearId);

        // Reuse the report service shortage calculation
        return reportService.getShortageReport(schoolId, yearId, sectionId)
                .stream()
                .map(s -> LowAttendanceAlertResponse.builder()
                        .studentId(s.getStudentId())
                        .sectionId(s.getSectionId())
                        .currentPercentage(s.getAttendancePercentage())
                        .threshold(threshold)
                        .shortfallPercentage(s.getShortfallPercentage())
                        .build())
                .toList();
    }

    // =========================================================================
    // FR-DASH-06: Section dashboard (teacher view)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SectionDailySummaryResponse getSectionDashboard(Long schoolId, Long sectionId) {
        LocalDate today = LocalDate.now();
        List<DailyAttendanceRecord> records =
                attendanceRepository.findBySectionIdAndAttendanceDate(sectionId, today);

        int enrolled = (int) enrollmentRepository.countBySectionId(sectionId);
        int present  = count(records, DailyAttendanceStatus.PRESENT);
        int late     = count(records, DailyAttendanceStatus.LATE);
        int absent   = count(records, DailyAttendanceStatus.ABSENT);
        int onLeave  = count(records, DailyAttendanceStatus.ON_LEAVE);

        int denom = present + late + absent;
        BigDecimal sectionPct = denom > 0 ? pct(present + late, denom) : BigDecimal.ZERO;

        return SectionDailySummaryResponse.builder()
                .sectionId(sectionId)
                .sectionName(null) // name looked up from section entity in controller if needed
                .totalStudents(enrolled)
                .present(present).late(late).absent(absent).onLeave(onLeave)
                .attendancePercentage(sectionPct)
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SchoolOverviewResponse emptyOverview(Long schoolId, LocalDate today, boolean isWorkingDay) {
        return SchoolOverviewResponse.builder()
                .date(today).workingDay(isWorkingDay).academicYearId(null)
                .totalStudents(0).present(0).late(0).absent(0).onLeave(0)
                .attendancePercentage(BigDecimal.ZERO)
                .pendingActions(PendingActionsResponse.builder()
                        .correctionRequests(0).leaveApplications(0).consecutiveAbsenceAlerts(0)
                        .build())
                .build();
    }

    private int count(List<DailyAttendanceRecord> records, DailyAttendanceStatus status) {
        return (int) records.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private BigDecimal pct(int numerator, int denominator) {
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
