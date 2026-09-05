package com.attendai.school.attendancereports.service;

import com.attendai.school.academiccalendar.service.AcademicCalendarService;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.attendancereports.dto.AttendanceShortageResponse;
import com.attendai.school.attendancereports.dto.ConsecutiveAbsenceResponse;
import com.attendai.school.attendancereports.dto.StudentAttendanceSummaryResponse;
import com.attendai.school.attendancerules.service.AttendanceRulesService;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import com.attendai.school.dailyattendance.repository.DailyAttendanceRepository;
import com.attendai.school.section.entity.SectionEnrollment;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceReportServiceImplTest {

    @Mock DailyAttendanceRepository   attendanceRepository;
    @Mock SectionEnrollmentRepository enrollmentRepository;
    @Mock AcademicYearService         academicYearService;
    @Mock AcademicCalendarService     calendarService;
    @Mock AttendanceRulesService      rulesService;

    private AttendanceReportServiceImpl service;

    private static final Long SCHOOL_ID  = 1L;
    private static final Long YEAR_ID    = 10L;
    private static final Long STUDENT_ID = 30L;
    private static final LocalDate FROM  = LocalDate.of(2025, 6, 1);
    private static final LocalDate TO    = LocalDate.of(2025, 10, 31);

    @BeforeEach
    void setUp() {
        service = new AttendanceReportServiceImpl(attendanceRepository, enrollmentRepository,
                academicYearService, calendarService, rulesService);
    }

    // =========================================================================
    // getStudentSummary — percentage formula
    // =========================================================================

    @Test
    void studentSummary_shouldCalculateCorrectPercentage_withPresentsAndLates() {
        stubYear();
        // 8 present + 2 late + 2 absent, 0 on_leave; workingDays=12
        List<DailyAttendanceRecord> records = List.of(
                rec(DailyAttendanceStatus.PRESENT, FROM),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(1)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(2)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(3)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(4)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(5)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(6)),
                rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(7)),
                rec(DailyAttendanceStatus.LATE,    FROM.plusDays(8)),
                rec(DailyAttendanceStatus.LATE,    FROM.plusDays(9)),
                rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(10)),
                rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(11))
        );
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(records);
        when(calendarService.getWorkingDayCount(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(12);
        when(rulesService.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID))
                .thenReturn(new BigDecimal("75.00"));

        StudentAttendanceSummaryResponse result =
                service.getStudentSummary(SCHOOL_ID, STUDENT_ID, YEAR_ID, FROM, TO);

        // (8+2)/12 * 100 = 83.33
        assertThat(result.getAttendancePercentage()).isEqualByComparingTo("83.33");
        assertThat(result.getPresentDays()).isEqualTo(8);
        assertThat(result.getLateDays()).isEqualTo(2);
        assertThat(result.getAbsentDays()).isEqualTo(2);
        assertThat(result.isBelowThreshold()).isFalse();
    }

    @Test
    void studentSummary_shouldExcludeOnLeaveFromDenominator() {
        stubYear();
        // 6 present + 0 late + 2 absent + 2 on_leave; workingDays=10; denominator=8
        List<DailyAttendanceRecord> records = List.of(
                rec(DailyAttendanceStatus.PRESENT,  FROM),
                rec(DailyAttendanceStatus.PRESENT,  FROM.plusDays(1)),
                rec(DailyAttendanceStatus.PRESENT,  FROM.plusDays(2)),
                rec(DailyAttendanceStatus.PRESENT,  FROM.plusDays(3)),
                rec(DailyAttendanceStatus.PRESENT,  FROM.plusDays(4)),
                rec(DailyAttendanceStatus.PRESENT,  FROM.plusDays(5)),
                rec(DailyAttendanceStatus.ABSENT,   FROM.plusDays(6)),
                rec(DailyAttendanceStatus.ABSENT,   FROM.plusDays(7)),
                rec(DailyAttendanceStatus.ON_LEAVE, FROM.plusDays(8)),
                rec(DailyAttendanceStatus.ON_LEAVE, FROM.plusDays(9))
        );
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(records);
        when(calendarService.getWorkingDayCount(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(10);
        when(rulesService.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID))
                .thenReturn(new BigDecimal("75.00"));

        StudentAttendanceSummaryResponse result =
                service.getStudentSummary(SCHOOL_ID, STUDENT_ID, YEAR_ID, FROM, TO);

        // denominator = 10 - 2 = 8; (6+0)/8*100 = 75.00
        assertThat(result.getAttendancePercentage()).isEqualByComparingTo("75.00");
        assertThat(result.isBelowThreshold()).isFalse();
    }

    @Test
    void studentSummary_shouldMarkBelowThreshold_whenPercentageLow() {
        stubYear();
        List<DailyAttendanceRecord> records = List.of(
                rec(DailyAttendanceStatus.PRESENT, FROM),
                rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(1)),
                rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(2)),
                rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(3))
        );
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(records);
        when(calendarService.getWorkingDayCount(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(4);
        when(rulesService.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID))
                .thenReturn(new BigDecimal("75.00"));

        StudentAttendanceSummaryResponse result =
                service.getStudentSummary(SCHOOL_ID, STUDENT_ID, YEAR_ID, FROM, TO);

        // 1/4*100 = 25% < 75% → belowThreshold
        assertThat(result.getAttendancePercentage()).isEqualByComparingTo("25.00");
        assertThat(result.isBelowThreshold()).isTrue();
    }

    // =========================================================================
    // getShortageReport
    // =========================================================================

    @Test
    void shortageReport_shouldIncludeOnlyStudentsBelowThreshold() {
        stubYear();

        SectionEnrollment e1 = enrollment(STUDENT_ID,    20L);
        SectionEnrollment e2 = enrollment(STUDENT_ID + 1, 20L);
        when(enrollmentRepository.findByAcademicYearId(YEAR_ID)).thenReturn(List.of(e1, e2));
        when(rulesService.getMinAttendancePercentage(SCHOOL_ID, YEAR_ID))
                .thenReturn(new BigDecimal("75.00"));

        // Student 30: 1 present out of 4 working days = 25% → below
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(List.of(
                        rec(DailyAttendanceStatus.PRESENT, FROM),
                        rec(DailyAttendanceStatus.ABSENT, FROM.plusDays(1)),
                        rec(DailyAttendanceStatus.ABSENT, FROM.plusDays(2)),
                        rec(DailyAttendanceStatus.ABSENT, FROM.plusDays(3))));
        // Student 31: 4 present out of 4 = 100% → above
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID + 1), any(), any())).thenReturn(List.of(
                        rec(DailyAttendanceStatus.PRESENT, FROM),
                        rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(1)),
                        rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(2)),
                        rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(3))));
        when(calendarService.getWorkingDayCount(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(4);

        List<AttendanceShortageResponse> result =
                service.getShortageReport(SCHOOL_ID, YEAR_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudentId()).isEqualTo(STUDENT_ID);
    }

    // =========================================================================
    // getConsecutiveAbsences
    // =========================================================================

    @Test
    void consecutiveAbsences_shouldDetectStreak() {
        stubYear();
        when(calendarService.getWorkingDates(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(List.of(FROM, FROM.plusDays(1), FROM.plusDays(2),
                                    FROM.plusDays(3), FROM.plusDays(4)));
        SectionEnrollment e = enrollment(STUDENT_ID, 20L);
        when(enrollmentRepository.findBySectionIdOrderByRollNumberAsc(20L)).thenReturn(List.of(e));

        // Days 1,2,3 absent, day 4 present, day 5 absent
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(List.of(
                        rec(DailyAttendanceStatus.ABSENT,  FROM),
                        rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(1)),
                        rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(2)),
                        rec(DailyAttendanceStatus.PRESENT, FROM.plusDays(3)),
                        rec(DailyAttendanceStatus.ABSENT,  FROM.plusDays(4))));

        List<ConsecutiveAbsenceResponse> result =
                service.getConsecutiveAbsences(SCHOOL_ID, YEAR_ID, 20L, 3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConsecutiveDays()).isEqualTo(3);
        assertThat(result.get(0).getStreakStartDate()).isEqualTo(FROM);
    }

    @Test
    void consecutiveAbsences_shouldReturnEmpty_whenStreakBelowThreshold() {
        stubYear();
        when(calendarService.getWorkingDates(eq(SCHOOL_ID), eq(YEAR_ID), any(), any()))
                .thenReturn(List.of(FROM, FROM.plusDays(1)));
        SectionEnrollment e = enrollment(STUDENT_ID, 20L);
        when(enrollmentRepository.findBySectionIdOrderByRollNumberAsc(20L)).thenReturn(List.of(e));
        when(attendanceRepository.findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                eq(STUDENT_ID), any(), any())).thenReturn(List.of(
                        rec(DailyAttendanceStatus.ABSENT, FROM),
                        rec(DailyAttendanceStatus.ABSENT, FROM.plusDays(1))));

        // Only 2 consecutive but threshold is 3
        List<ConsecutiveAbsenceResponse> result =
                service.getConsecutiveAbsences(SCHOOL_ID, YEAR_ID, 20L, 3);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubYear() {
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(
                AcademicYearResponse.builder().id(YEAR_ID).schoolId(SCHOOL_ID).name("2025-2026")
                        .startDate(FROM).endDate(LocalDate.of(2026, 3, 31))
                        .status(AcademicYearStatus.ACTIVE)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    private DailyAttendanceRecord rec(DailyAttendanceStatus status, LocalDate date) {
        DailyAttendanceRecord r = DailyAttendanceRecord.builder()
                .schoolId(SCHOOL_ID).academicYearId(YEAR_ID).sectionId(20L)
                .studentId(STUDENT_ID).attendanceDate(date).status(status).build();
        r.setId((long) (Math.random() * 10000));
        return r;
    }

    private SectionEnrollment enrollment(Long studentId, Long sectionId) {
        SectionEnrollment e = SectionEnrollment.builder()
                .studentId(studentId).sectionId(sectionId).academicYearId(YEAR_ID)
                .rollNumber("01").enrolledAt(FROM).build();
        e.setId(1L);
        return e;
    }
}
