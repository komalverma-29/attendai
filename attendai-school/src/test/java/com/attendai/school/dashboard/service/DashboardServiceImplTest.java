package com.attendai.school.dashboard.service;

import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.attendancecorrections.repository.AttendanceCorrectionRepository;
import com.attendai.school.attendancereports.dto.AttendanceShortageResponse;
import com.attendai.school.attendancereports.service.AttendanceReportService;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dashboard.dto.SchoolOverviewResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.repository.LeaveApplicationRepository;
import com.attendai.school.section.repository.SchoolSectionRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock DailyAttendanceRepository      attendanceRepository;
    @Mock SchoolSectionRepository        sectionRepository;
    @Mock SectionEnrollmentRepository    enrollmentRepository;
    @Mock AcademicYearService            academicYearService;
    @Mock AcademicCalendarService        calendarService;
    @Mock AttendanceRulesService         rulesService;
    @Mock AttendanceReportService        reportService;
    @Mock LeaveApplicationRepository     leaveRepository;
    @Mock AttendanceCorrectionRepository correctionRepository;

    private DashboardServiceImpl service;

    private static final Long SCHOOL_ID = 1L;
    private static final Long YEAR_ID   = 10L;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(
                attendanceRepository, sectionRepository, enrollmentRepository,
                academicYearService, calendarService, rulesService, reportService,
                leaveRepository, correctionRepository);
    }

    // =========================================================================
    // getSchoolOverview
    // =========================================================================

    @Test
    void getSchoolOverview_shouldReturnZeros_whenNotWorkingDay() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID))
                .thenReturn(Optional.of(buildYear()));
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any()))
                .thenReturn(false);

        SchoolOverviewResponse resp = service.getSchoolOverview(SCHOOL_ID);

        assertThat(resp.isWorkingDay()).isFalse();
        assertThat(resp.getTotalStudents()).isEqualTo(0);
        assertThat(resp.getPresent()).isEqualTo(0);
        assertThat(resp.getAttendancePercentage()).isEqualByComparingTo("0");
    }

    @Test
    void getSchoolOverview_shouldReturnZeros_whenNoActiveYear() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID)).thenReturn(Optional.empty());

        SchoolOverviewResponse resp = service.getSchoolOverview(SCHOOL_ID);

        assertThat(resp.isWorkingDay()).isFalse();
        assertThat(resp.getTotalStudents()).isEqualTo(0);
    }

    @Test
    void getSchoolOverview_shouldReturnCorrectCounts_whenWorkingDay() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID))
                .thenReturn(Optional.of(buildYear()));
        when(calendarService.isWorkingDay(eq(SCHOOL_ID), eq(YEAR_ID), any())).thenReturn(true);

        LocalDate today = LocalDate.now();
        List<DailyAttendanceRecord> records = List.of(
                record(1L, DailyAttendanceStatus.PRESENT),
                record(2L, DailyAttendanceStatus.PRESENT),
                record(3L, DailyAttendanceStatus.LATE),
                record(4L, DailyAttendanceStatus.ABSENT)
        );
        when(attendanceRepository.findBySchoolIdAndDateRange(eq(SCHOOL_ID), any(), any()))
                .thenReturn(records);
        when(correctionRepository.countBySchoolIdAndStatus(SCHOOL_ID, CorrectionStatus.PENDING))
                .thenReturn(2L);
        when(leaveRepository.countBySchoolIdAndStatus(SCHOOL_ID, LeaveStatus.PENDING))
                .thenReturn(5L);
        when(reportService.getConsecutiveAbsences(anyLong(), anyLong(), any(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(rulesService.getConsecutiveAbsenceAlert(anyLong(), anyLong())).thenReturn(3);

        SchoolOverviewResponse resp = service.getSchoolOverview(SCHOOL_ID);

        assertThat(resp.isWorkingDay()).isTrue();
        assertThat(resp.getPresent()).isEqualTo(2);
        assertThat(resp.getLate()).isEqualTo(1);
        assertThat(resp.getAbsent()).isEqualTo(1);
        // (2+1)/3 * 100 = 75% (denominator = 3: present+late+absent)
        assertThat(resp.getAttendancePercentage()).isEqualByComparingTo("75.00");
        assertThat(resp.getPendingActions().getCorrectionRequests()).isEqualTo(2);
        assertThat(resp.getPendingActions().getLeaveApplications()).isEqualTo(5);
    }

    // =========================================================================
    // getAttendanceTrend
    // =========================================================================

    @Test
    void getAttendanceTrend_shouldReturnOnlyWorkingDays() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID))
                .thenReturn(Optional.of(buildYear()));

        // Only 2 working days in range
        LocalDate d1 = LocalDate.now().minusDays(5);
        LocalDate d2 = LocalDate.now().minusDays(3);
        when(calendarService.getWorkingDates(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(List.of(d1, d2));

        // Records for each date
        when(attendanceRepository.findBySchoolIdAndDateRange(eq(SCHOOL_ID), eq(d1), eq(d1)))
                .thenReturn(List.of(record(1L, DailyAttendanceStatus.PRESENT)));
        when(attendanceRepository.findBySchoolIdAndDateRange(eq(SCHOOL_ID), eq(d2), eq(d2)))
                .thenReturn(List.of(record(2L, DailyAttendanceStatus.ABSENT)));

        var resp = service.getAttendanceTrend(SCHOOL_ID, null);

        assertThat(resp.getTrend()).hasSize(2);
        assertThat(resp.getTrend().get(0).getDate()).isEqualTo(d1);
        // 1 present / 1 total (denom excludes onLeave) = 100%
        assertThat(resp.getTrend().get(0).getPercentage()).isEqualByComparingTo("100.00");
        // 0 present, 1 absent → 0%
        assertThat(resp.getTrend().get(1).getPercentage()).isEqualByComparingTo("0.00");
    }

    // =========================================================================
    // getLowAttendanceAlerts
    // =========================================================================

    @Test
    void getLowAttendanceAlerts_shouldDelegateToReportService() {
        when(academicYearService.getActiveAcademicYear(SCHOOL_ID))
                .thenReturn(Optional.of(buildYear()));
        when(rulesService.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID))
                .thenReturn(new BigDecimal("75.00"));
        when(reportService.getShortageReport(SCHOOL_ID, YEAR_ID, null))
                .thenReturn(List.of(
                        AttendanceShortageResponse.builder()
                                .studentId(30L).sectionId(20L)
                                .attendancePercentage(new BigDecimal("60.00"))
                                .minimumRequired(new BigDecimal("75.00"))
                                .shortfallPercentage(new BigDecimal("15.00"))
                                .build()));

        var alerts = service.getLowAttendanceAlerts(SCHOOL_ID, null);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getStudentId()).isEqualTo(30L);
        assertThat(alerts.get(0).getCurrentPercentage()).isEqualByComparingTo("60.00");
    }

    // =========================================================================
    // getSectionDashboard
    // =========================================================================

    @Test
    void getSectionDashboard_shouldAggregateCorrectly() {
        when(attendanceRepository.findBySectionIdAndAttendanceDate(eq(20L), any()))
                .thenReturn(List.of(
                        record(1L, DailyAttendanceStatus.PRESENT),
                        record(2L, DailyAttendanceStatus.LATE),
                        record(3L, DailyAttendanceStatus.ABSENT)));
        when(enrollmentRepository.countBySectionId(20L)).thenReturn(30L);

        var resp = service.getSectionDashboard(SCHOOL_ID, 20L);

        assertThat(resp.getPresent()).isEqualTo(1);
        assertThat(resp.getLate()).isEqualTo(1);
        assertThat(resp.getAbsent()).isEqualTo(1);
        assertThat(resp.getTotalStudents()).isEqualTo(30);
        // (1+1)/3*100 = 66.67
        assertThat(resp.getAttendancePercentage()).isEqualByComparingTo("66.67");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AcademicYearResponse buildYear() {
        return AcademicYearResponse.builder()
                .id(YEAR_ID).schoolId(SCHOOL_ID).name("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1)).endDate(LocalDate.of(2026, 3, 31))
                .status(AcademicYearStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private DailyAttendanceRecord record(Long studentId, DailyAttendanceStatus status) {
        DailyAttendanceRecord r = DailyAttendanceRecord.builder()
                .schoolId(SCHOOL_ID).academicYearId(YEAR_ID).sectionId(20L)
                .studentId(studentId).attendanceDate(LocalDate.now()).status(status).build();
        r.setId(studentId * 100);
        return r;
    }
}
