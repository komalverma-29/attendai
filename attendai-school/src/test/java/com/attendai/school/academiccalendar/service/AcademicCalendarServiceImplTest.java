package com.attendai.school.academiccalendar.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.cache.CalendarCache;
import com.attendai.school.academiccalendar.dto.CreateHolidayRangeRequest;
import com.attendai.school.academiccalendar.dto.CreateHolidayRequest;
import com.attendai.school.academiccalendar.dto.DeclareWorkingDayRequest;
import com.attendai.school.academiccalendar.dto.UpdateCalendarEntryRequest;
import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import com.attendai.school.academiccalendar.entity.SchoolCalendarEntry;
import com.attendai.school.academiccalendar.exception.CalendarEntryNotFoundException;
import com.attendai.school.academiccalendar.mapper.AcademicCalendarMapper;
import com.attendai.school.academiccalendar.repository.SchoolCalendarEntryRepository;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.settings.service.SchoolSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarServiceImplTest {

    @Mock SchoolCalendarEntryRepository calendarRepository;
    @Mock AcademicCalendarMapper        calendarMapper;
    @Mock AcademicYearService           academicYearService;
    @Mock SchoolSettingsService         settingsService;
    @Mock CalendarCache                 calendarCache;
    @Mock AuditService                  auditService;

    private AcademicCalendarServiceImpl service;

    // Academic year with ACTIVE status covering Jun 2025 – Mar 2026
    private static final Long SCHOOL_ID = 1L;
    private static final Long YEAR_ID   = 10L;
    private static final LocalDate YEAR_START = LocalDate.of(2025, 6, 1);
    private static final LocalDate YEAR_END   = LocalDate.of(2026, 3, 31);

    @BeforeEach
    void setUp() {
        service = new AcademicCalendarServiceImpl(
                calendarRepository, calendarMapper, academicYearService,
                settingsService, calendarCache, auditService);
    }

    // =========================================================================
    // createHoliday
    // =========================================================================

    @Test
    void createHoliday_shouldSave_whenValidDate() {
        stubActiveYear();
        LocalDate date = LocalDate.of(2025, 10, 24);
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, date)).thenReturn(false);
        SchoolCalendarEntry saved = buildEntry(1L, SCHOOL_ID, YEAR_ID, date, CalendarEntryType.HOLIDAY);
        when(calendarRepository.save(any())).thenReturn(saved);
        when(calendarMapper.toResponse(saved)).thenReturn(null);

        service.createHoliday(SCHOOL_ID, YEAR_ID, buildHolidayRequest(date));

        verify(calendarRepository).save(any(SchoolCalendarEntry.class));
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
        verify(auditService).log(any());
    }

    @Test
    void createHoliday_shouldThrow_whenDateBeforeYearStart() {
        stubActiveYear();
        LocalDate dateBeforeYear = YEAR_START.minusDays(1);

        assertThatThrownBy(() -> service.createHoliday(SCHOOL_ID, YEAR_ID,
                buildHolidayRequest(dateBeforeYear)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("outside the academic year range");
        verify(calendarRepository, never()).save(any());
    }

    @Test
    void createHoliday_shouldThrow_whenDateAfterYearEnd() {
        stubActiveYear();
        LocalDate dateAfterYear = YEAR_END.plusDays(1);

        assertThatThrownBy(() -> service.createHoliday(SCHOOL_ID, YEAR_ID,
                buildHolidayRequest(dateAfterYear)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createHoliday_shouldThrow_whenDateExactlyOnYearStart() {
        // Start date IS within range — should succeed
        stubActiveYear();
        LocalDate startDate = YEAR_START;
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, startDate)).thenReturn(false);
        when(calendarRepository.save(any())).thenReturn(
                buildEntry(1L, SCHOOL_ID, YEAR_ID, startDate, CalendarEntryType.HOLIDAY));
        when(calendarMapper.toResponse(any())).thenReturn(null);

        service.createHoliday(SCHOOL_ID, YEAR_ID, buildHolidayRequest(startDate));
        verify(calendarRepository).save(any());
    }

    @Test
    void createHoliday_shouldThrow409_whenDuplicateDate() {
        stubActiveYear();
        LocalDate date = LocalDate.of(2025, 10, 24);
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, date)).thenReturn(true);

        assertThatThrownBy(() -> service.createHoliday(SCHOOL_ID, YEAR_ID,
                buildHolidayRequest(date)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createHoliday_shouldThrow_whenAcademicYearCompleted() {
        stubYearWithStatus(AcademicYearStatus.COMPLETED);

        assertThatThrownBy(() -> service.createHoliday(SCHOOL_ID, YEAR_ID,
                buildHolidayRequest(LocalDate.of(2025, 10, 24))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
        verify(calendarRepository, never()).save(any());
    }

    @Test
    void createHoliday_shouldThrow_whenAcademicYearCancelled() {
        stubYearWithStatus(AcademicYearStatus.CANCELLED);

        assertThatThrownBy(() -> service.createHoliday(SCHOOL_ID, YEAR_ID,
                buildHolidayRequest(LocalDate.of(2025, 10, 24))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void createHoliday_shouldSucceed_whenYearIsUpcoming() {
        stubYearWithStatus(AcademicYearStatus.UPCOMING);
        LocalDate date = LocalDate.of(2025, 10, 24);
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, date)).thenReturn(false);
        when(calendarRepository.save(any())).thenReturn(
                buildEntry(1L, SCHOOL_ID, YEAR_ID, date, CalendarEntryType.HOLIDAY));
        when(calendarMapper.toResponse(any())).thenReturn(null);

        service.createHoliday(SCHOOL_ID, YEAR_ID, buildHolidayRequest(date));
        verify(calendarRepository).save(any());
    }

    // =========================================================================
    // createHolidayRange
    // =========================================================================

    @Test
    void createHolidayRange_shouldCreateOneEntryPerDay_whenNoDuplicates() {
        stubActiveYear();
        LocalDate start = LocalDate.of(2025, 10, 24);
        LocalDate end   = LocalDate.of(2025, 10, 26);  // 3 days

        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                anyLong(), anyLong(), any())).thenReturn(false);
        when(calendarRepository.save(any())).thenAnswer(inv -> {
            SchoolCalendarEntry e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(calendarMapper.toResponse(any())).thenReturn(null);

        CreateHolidayRangeRequest req = new CreateHolidayRangeRequest();
        req.setStartDate(start); req.setEndDate(end); req.setName("Break");
        var results = service.createHolidayRange(SCHOOL_ID, YEAR_ID, req);

        assertThat(results).hasSize(3);
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
    }

    @Test
    void createHolidayRange_shouldSkipExistingDates() {
        stubActiveYear();
        LocalDate start = LocalDate.of(2025, 10, 24);
        LocalDate end   = LocalDate.of(2025, 10, 25);

        // Oct 24 already exists
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, start)).thenReturn(true);
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, end)).thenReturn(false);
        when(calendarRepository.save(any())).thenAnswer(inv -> {
            SchoolCalendarEntry e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });
        when(calendarMapper.toResponse(any())).thenReturn(null);

        CreateHolidayRangeRequest req = new CreateHolidayRangeRequest();
        req.setStartDate(start); req.setEndDate(end); req.setName("Break");
        var results = service.createHolidayRange(SCHOOL_ID, YEAR_ID, req);

        assertThat(results).hasSize(1); // only Oct 25 created
    }

    // =========================================================================
    // deleteEntry
    // =========================================================================

    @Test
    void deleteEntry_shouldHardDelete_whenFound() {
        stubActiveYear();
        SchoolCalendarEntry entry = buildEntry(1L, SCHOOL_ID, YEAR_ID,
                LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY);
        when(calendarRepository.findById(1L)).thenReturn(Optional.of(entry));

        service.deleteEntry(SCHOOL_ID, 1L);

        verify(calendarRepository).delete(entry);
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
        verify(auditService).log(any());
    }

    @Test
    void deleteEntry_shouldThrow404_whenNotFound() {
        when(calendarRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteEntry(SCHOOL_ID, 99L))
                .isInstanceOf(CalendarEntryNotFoundException.class);
    }

    @Test
    void deleteEntry_shouldThrow404_whenEntryBelongsToDifferentSchool() {
        SchoolCalendarEntry entry = buildEntry(1L, 99L, YEAR_ID,
                LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY); // school 99
        when(calendarRepository.findById(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.deleteEntry(SCHOOL_ID, 1L))
                .isInstanceOf(CalendarEntryNotFoundException.class);
    }

    // =========================================================================
    // declareWorkingDay
    // =========================================================================

    @Test
    void declareWorkingDay_shouldSaveAsWorkingDay() {
        stubActiveYear();
        LocalDate saturday = LocalDate.of(2025, 11, 8); // a Saturday
        when(calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                SCHOOL_ID, YEAR_ID, saturday)).thenReturn(false);
        when(calendarRepository.save(any())).thenReturn(
                buildEntry(1L, SCHOOL_ID, YEAR_ID, saturday, CalendarEntryType.WORKING_DAY));
        when(calendarMapper.toResponse(any())).thenReturn(null);

        DeclareWorkingDayRequest req = new DeclareWorkingDayRequest();
        req.setDate(saturday); req.setName("Makeup Saturday");
        service.declareWorkingDay(SCHOOL_ID, YEAR_ID, req);

        verify(calendarRepository).save(any());
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
    }

    // =========================================================================
    // convertToWorkingDay
    // =========================================================================

    @Test
    void convertToWorkingDay_shouldChangeType_whenHoliday() {
        stubActiveYear();
        SchoolCalendarEntry entry = buildEntry(1L, SCHOOL_ID, YEAR_ID,
                LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY);
        when(calendarRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(calendarRepository.save(any())).thenReturn(entry);
        when(calendarMapper.toResponse(any())).thenReturn(null);

        service.convertToWorkingDay(SCHOOL_ID, 1L);

        assertThat(entry.getEntryType()).isEqualTo(CalendarEntryType.WORKING_DAY);
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
    }

    // =========================================================================
    // isWorkingDay — the core algorithm
    // =========================================================================

    @Test
    void isWorkingDay_shouldReturnFalse_whenHolidayEntry() {
        // Cache miss → DB returns a HOLIDAY entry for that date
        when(calendarCache.get(SCHOOL_ID, YEAR_ID)).thenReturn(Optional.empty());
        when(calendarRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(List.of(buildEntry(1L, SCHOOL_ID, YEAR_ID,
                        LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY)));

        boolean result = service.isWorkingDay(SCHOOL_ID, YEAR_ID, LocalDate.of(2025, 10, 24));

        assertThat(result).isFalse();
    }

    @Test
    void isWorkingDay_shouldReturnTrue_whenWorkingDayEntry_onWeekend() {
        // Saturday with a WORKING_DAY entry → true
        LocalDate saturday = LocalDate.of(2025, 11, 8);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID)).thenReturn(Optional.empty());
        when(calendarRepository.findBySchoolIdAndAcademicYearId(SCHOOL_ID, YEAR_ID))
                .thenReturn(List.of(buildEntry(1L, SCHOOL_ID, YEAR_ID,
                        saturday, CalendarEntryType.WORKING_DAY)));

        boolean result = service.isWorkingDay(SCHOOL_ID, YEAR_ID, saturday);

        assertThat(result).isTrue();
    }

    @Test
    void isWorkingDay_shouldReturnFalse_whenWeekend_noEntry() {
        // No entry for a Saturday → fall back to weekend rule
        LocalDate saturday = LocalDate.of(2025, 11, 8);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(Collections.emptyList()));
        when(settingsService.getWeekendDays(SCHOOL_ID))
                .thenReturn(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        boolean result = service.isWorkingDay(SCHOOL_ID, YEAR_ID, saturday);

        assertThat(result).isFalse();
    }

    @Test
    void isWorkingDay_shouldReturnTrue_whenWeekday_noEntry() {
        // No entry for a Wednesday → true (regular working day)
        LocalDate wednesday = LocalDate.of(2025, 10, 22);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(Collections.emptyList()));
        when(settingsService.getWeekendDays(SCHOOL_ID))
                .thenReturn(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        boolean result = service.isWorkingDay(SCHOOL_ID, YEAR_ID, wednesday);

        assertThat(result).isTrue();
    }

    @Test
    void isWorkingDay_shouldReturnTrue_whenCacheHit_workingDay() {
        // Cache already loaded with an empty list → weekday → true
        LocalDate monday = LocalDate.of(2025, 11, 3);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(Collections.emptyList()));
        when(settingsService.getWeekendDays(SCHOOL_ID))
                .thenReturn(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        assertThat(service.isWorkingDay(SCHOOL_ID, YEAR_ID, monday)).isTrue();
        // Repository should NOT be called — cache was hit
        verify(calendarRepository, never()).findBySchoolIdAndAcademicYearId(any(), any());
    }

    // =========================================================================
    // getWorkingDayCount — internal API
    // =========================================================================

    @Test
    void getWorkingDayCount_shouldCountCorrectly_withHolidayAndWeekend() {
        // Week of 2025-10-20 (Mon) to 2025-10-26 (Sun)
        // Oct 24 Fri = holiday, Oct 25 Sat = weekend, Oct 26 Sun = weekend
        // Working: Mon 20, Tue 21, Wed 22, Thu 23 = 4 working days
        LocalDate from = LocalDate.of(2025, 10, 20);
        LocalDate to   = LocalDate.of(2025, 10, 26);

        SchoolCalendarEntry holiday = buildEntry(1L, SCHOOL_ID, YEAR_ID,
                LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(List.of(holiday)));
        when(settingsService.getWeekendDays(SCHOOL_ID))
                .thenReturn(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));

        int count = service.getWorkingDayCount(SCHOOL_ID, YEAR_ID, from, to);

        assertThat(count).isEqualTo(4);
    }

    @Test
    void getWorkingDayCount_shouldCountMakeSaturdayAsWorkingDay() {
        // Week with a declared working Saturday
        LocalDate from = LocalDate.of(2025, 11, 8);  // Saturday
        LocalDate to   = LocalDate.of(2025, 11, 8);

        SchoolCalendarEntry makeSat = buildEntry(1L, SCHOOL_ID, YEAR_ID,
                from, CalendarEntryType.WORKING_DAY);
        when(calendarCache.get(SCHOOL_ID, YEAR_ID))
                .thenReturn(Optional.of(List.of(makeSat)));
        // settingsService not called when entry found

        int count = service.getWorkingDayCount(SCHOOL_ID, YEAR_ID, from, to);

        assertThat(count).isEqualTo(1);
    }

    // =========================================================================
    // updateEntry
    // =========================================================================

    @Test
    void updateEntry_shouldUpdateNameAndDescription() {
        stubActiveYear();
        SchoolCalendarEntry entry = buildEntry(1L, SCHOOL_ID, YEAR_ID,
                LocalDate.of(2025, 10, 24), CalendarEntryType.HOLIDAY);
        when(calendarRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(calendarRepository.save(any())).thenReturn(entry);
        when(calendarMapper.toResponse(any())).thenReturn(null);

        UpdateCalendarEntryRequest req = new UpdateCalendarEntryRequest();
        req.setName("Updated Name");
        service.updateEntry(SCHOOL_ID, 1L, req);

        assertThat(entry.getName()).isEqualTo("Updated Name");
        verify(calendarCache).evict(SCHOOL_ID, YEAR_ID);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubActiveYear() {
        stubYearWithStatus(AcademicYearStatus.ACTIVE);
    }

    private void stubYearWithStatus(AcademicYearStatus status) {
        when(academicYearService.findById(SCHOOL_ID, YEAR_ID)).thenReturn(
                AcademicYearResponse.builder()
                        .id(YEAR_ID).schoolId(SCHOOL_ID)
                        .name("2025-2026")
                        .startDate(YEAR_START).endDate(YEAR_END)
                        .status(status)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                        .build());
    }

    private SchoolCalendarEntry buildEntry(Long id, Long schoolId, Long yearId,
                                            LocalDate date, CalendarEntryType type) {
        SchoolCalendarEntry e = SchoolCalendarEntry.builder()
                .schoolId(schoolId).academicYearId(yearId)
                .entryDate(date).entryType(type).name("Test").build();
        e.setId(id);
        return e;
    }

    private CreateHolidayRequest buildHolidayRequest(LocalDate date) {
        CreateHolidayRequest req = new CreateHolidayRequest();
        req.setDate(date);
        req.setName("Holiday");
        return req;
    }
}
