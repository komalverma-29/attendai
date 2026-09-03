package com.attendai.school.academiccalendar.service;

import com.attendai.school.academiccalendar.dto.CalendarEntryResponse;
import com.attendai.school.academiccalendar.dto.CreateHolidayRangeRequest;
import com.attendai.school.academiccalendar.dto.CreateHolidayRequest;
import com.attendai.school.academiccalendar.dto.DeclareWorkingDayRequest;
import com.attendai.school.academiccalendar.dto.MonthCalendarResponse;
import com.attendai.school.academiccalendar.dto.UpdateCalendarEntryRequest;
import com.attendai.school.academiccalendar.dto.WorkingDayCountResponse;
import com.attendai.school.academiccalendar.entity.CalendarEntryType;

import java.time.LocalDate;
import java.util.List;

public interface AcademicCalendarService {

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    CalendarEntryResponse createHoliday(Long schoolId, Long academicYearId,
                                         CreateHolidayRequest request);

    List<CalendarEntryResponse> createHolidayRange(Long schoolId, Long academicYearId,
                                                     CreateHolidayRangeRequest request);

    CalendarEntryResponse updateEntry(Long schoolId, Long entryId,
                                       UpdateCalendarEntryRequest request);

    void deleteEntry(Long schoolId, Long entryId);

    CalendarEntryResponse declareWorkingDay(Long schoolId, Long academicYearId,
                                             DeclareWorkingDayRequest request);

    CalendarEntryResponse convertToWorkingDay(Long schoolId, Long entryId);

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    MonthCalendarResponse getMonthCalendar(Long schoolId, Long academicYearId,
                                            int year, int month);

    List<CalendarEntryResponse> listEntries(Long schoolId, Long academicYearId,
                                             CalendarEntryType entryType,
                                             LocalDate fromDate, LocalDate toDate);

    WorkingDayCountResponse getWorkingDayCountResponse(Long schoolId, Long academicYearId,
                                                         LocalDate fromDate, LocalDate toDate);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-daily-attendance and attendance-reports
    // -------------------------------------------------------------------------

    /**
     * Returns true if the given date is a working school day for this school and year.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Check calendar entry: HOLIDAY → false, WORKING_DAY → true</li>
     *   <li>No entry: check school weekend days from settings</li>
     *   <li>Weekend day → false, weekday → true</li>
     * </ol>
     *
     * <p>This method uses the in-memory {@link com.attendai.school.academiccalendar.cache.CalendarCache}
     * and must respond in under 5ms.
     */
    boolean isWorkingDay(Long schoolId, Long academicYearId, LocalDate date);

    /**
     * Returns the number of working days in the given date range (inclusive).
     * Used by attendance report percentage calculations.
     */
    int getWorkingDayCount(Long schoolId, Long academicYearId,
                            LocalDate fromDate, LocalDate toDate);

    /**
     * Returns the list of working dates in the given range (inclusive).
     * Used by school-attendance-reports to iterate over working days.
     */
    List<LocalDate> getWorkingDates(Long schoolId, Long academicYearId,
                                     LocalDate fromDate, LocalDate toDate);
}
