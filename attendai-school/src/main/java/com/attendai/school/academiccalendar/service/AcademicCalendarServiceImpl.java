package com.attendai.school.academiccalendar.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academiccalendar.cache.CalendarCache;
import com.attendai.school.academiccalendar.dto.CalendarEntryResponse;
import com.attendai.school.academiccalendar.dto.CreateHolidayRangeRequest;
import com.attendai.school.academiccalendar.dto.CreateHolidayRequest;
import com.attendai.school.academiccalendar.dto.DeclareWorkingDayRequest;
import com.attendai.school.academiccalendar.dto.MonthCalendarResponse;
import com.attendai.school.academiccalendar.dto.MonthCalendarResponse.DayStatus;
import com.attendai.school.academiccalendar.dto.UpdateCalendarEntryRequest;
import com.attendai.school.academiccalendar.dto.WorkingDayCountResponse;
import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import com.attendai.school.academiccalendar.entity.SchoolCalendarEntry;
import com.attendai.school.academiccalendar.exception.CalendarEntryNotFoundException;
import com.attendai.school.academiccalendar.mapper.AcademicCalendarMapper;
import com.attendai.school.academiccalendar.repository.SchoolCalendarEntryRepository;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.settings.service.SchoolSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicCalendarServiceImpl implements AcademicCalendarService {

    private static final String MODULE = "school";

    private final SchoolCalendarEntryRepository calendarRepository;
    private final AcademicCalendarMapper        calendarMapper;
    private final AcademicYearService           academicYearService;
    private final SchoolSettingsService         settingsService;
    private final CalendarCache                 calendarCache;
    private final AuditService                  auditService;

    // =========================================================================
    // Write operations
    // =========================================================================

    @Override
    @Transactional
    public CalendarEntryResponse createHoliday(Long schoolId, Long academicYearId,
                                                CreateHolidayRequest request) {
        var year = validateWritableYear(schoolId, academicYearId);
        validateDateInYearRange(request.getDate(), year.getStartDate(), year.getEndDate());
        assertNoDuplicate(schoolId, academicYearId, request.getDate());

        SchoolCalendarEntry entry = SchoolCalendarEntry.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .entryDate(request.getDate())
                .entryType(CalendarEntryType.HOLIDAY)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        SchoolCalendarEntry saved = calendarRepository.save(entry);
        calendarCache.evict(schoolId, academicYearId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_HOLIDAY_CREATED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"date\":\"" + request.getDate()
                         + "\",\"name\":\"" + request.getName() + "\"}")
                .build());

        return calendarMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<CalendarEntryResponse> createHolidayRange(Long schoolId, Long academicYearId,
                                                            CreateHolidayRangeRequest request) {
        var year = validateWritableYear(schoolId, academicYearId);

        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();

        if (!end.isAfter(start) && !end.equals(start)) {
            throw new ValidationException("End date must be on or after start date");
        }
        validateDateInYearRange(start, year.getStartDate(), year.getEndDate());
        validateDateInYearRange(end,   year.getStartDate(), year.getEndDate());

        List<CalendarEntryResponse> results = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            // Skip dates that already have an entry (tolerate pre-existing entries in a range)
            if (!calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                    schoolId, academicYearId, current)) {
                SchoolCalendarEntry entry = SchoolCalendarEntry.builder()
                        .schoolId(schoolId)
                        .academicYearId(academicYearId)
                        .entryDate(current)
                        .entryType(CalendarEntryType.HOLIDAY)
                        .name(request.getName())
                        .description(request.getDescription())
                        .build();
                SchoolCalendarEntry saved = calendarRepository.save(entry);
                results.add(calendarMapper.toResponse(saved));
            }
            current = current.plusDays(1);
        }

        calendarCache.evict(schoolId, academicYearId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_HOLIDAY_CREATED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId("range")
                .details("{\"schoolId\":" + schoolId
                         + ",\"startDate\":\"" + start
                         + "\",\"endDate\":\"" + end
                         + "\",\"name\":\"" + request.getName() + "\"}")
                .build());

        return results;
    }

    @Override
    @Transactional
    public CalendarEntryResponse updateEntry(Long schoolId, Long entryId,
                                              UpdateCalendarEntryRequest request) {
        SchoolCalendarEntry entry = requireEntry(schoolId, entryId);
        validateWritableYear(schoolId, entry.getAcademicYearId());

        if (request.getName() != null)        entry.setName(request.getName());
        if (request.getDescription() != null) entry.setDescription(request.getDescription());

        SchoolCalendarEntry saved = calendarRepository.save(entry);
        calendarCache.evict(schoolId, entry.getAcademicYearId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_ENTRY_UPDATED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId(String.valueOf(entryId)).build());

        return calendarMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteEntry(Long schoolId, Long entryId) {
        SchoolCalendarEntry entry = requireEntry(schoolId, entryId);
        validateWritableYear(schoolId, entry.getAcademicYearId());

        Long academicYearId = entry.getAcademicYearId();
        LocalDate entryDate = entry.getEntryDate();

        calendarRepository.delete(entry);
        calendarCache.evict(schoolId, academicYearId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_ENTRY_DELETED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId(String.valueOf(entryId))
                .details("{\"date\":\"" + entryDate + "\"}")
                .build());
    }

    @Override
    @Transactional
    public CalendarEntryResponse declareWorkingDay(Long schoolId, Long academicYearId,
                                                    DeclareWorkingDayRequest request) {
        var year = validateWritableYear(schoolId, academicYearId);
        validateDateInYearRange(request.getDate(), year.getStartDate(), year.getEndDate());
        assertNoDuplicate(schoolId, academicYearId, request.getDate());

        SchoolCalendarEntry entry = SchoolCalendarEntry.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .entryDate(request.getDate())
                .entryType(CalendarEntryType.WORKING_DAY)
                .name(request.getName())
                .description(request.getDescription())
                .build();

        SchoolCalendarEntry saved = calendarRepository.save(entry);
        calendarCache.evict(schoolId, academicYearId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_WORKING_DAY_DECLARED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"date\":\"" + request.getDate() + "\"}")
                .build());

        return calendarMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CalendarEntryResponse convertToWorkingDay(Long schoolId, Long entryId) {
        SchoolCalendarEntry entry = requireEntry(schoolId, entryId);
        validateWritableYear(schoolId, entry.getAcademicYearId());

        entry.setEntryType(CalendarEntryType.WORKING_DAY);
        SchoolCalendarEntry saved = calendarRepository.save(entry);
        calendarCache.evict(schoolId, entry.getAcademicYearId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("CALENDAR_HOLIDAY_CONVERTED")
                .module(MODULE).resourceType("SchoolCalendarEntry")
                .resourceId(String.valueOf(entryId))
                .details("{\"date\":\"" + entry.getEntryDate() + "\"}")
                .build());

        return calendarMapper.toResponse(saved);
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public MonthCalendarResponse getMonthCalendar(Long schoolId, Long academicYearId,
                                                   int year, int month) {
        YearMonth ym       = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay  = ym.atEndOfMonth();

        // Load entries for this month window
        List<SchoolCalendarEntry> entries =
                calendarRepository.findBySchoolIdAndAcademicYearIdAndDateBetween(
                        schoolId, academicYearId, firstDay, lastDay);
        Map<LocalDate, SchoolCalendarEntry> entryMap =
                entries.stream().collect(Collectors.toMap(
                        SchoolCalendarEntry::getEntryDate, e -> e));

        Set<DayOfWeek> weekends = settingsService.getWeekendDays(schoolId);

        List<DayStatus> days = new ArrayList<>();
        LocalDate current = firstDay;
        while (!current.isAfter(lastDay)) {
            SchoolCalendarEntry e = entryMap.get(current);
            String status;
            String entryName = null;

            if (e != null) {
                status    = e.getEntryType().name();  // HOLIDAY or WORKING_DAY
                entryName = e.getName();
            } else if (weekends.contains(current.getDayOfWeek())) {
                status = "WEEKEND";
            } else {
                status = "WORKING_DAY";
            }

            days.add(DayStatus.builder()
                    .date(current)
                    .dayOfWeek(current.getDayOfWeek())
                    .status(status)
                    .entryName(entryName)
                    .build());
            current = current.plusDays(1);
        }

        return MonthCalendarResponse.builder()
                .year(year).month(month).days(days).build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEntryResponse> listEntries(Long schoolId, Long academicYearId,
                                                    CalendarEntryType entryType,
                                                    LocalDate fromDate, LocalDate toDate) {
        return calendarRepository.findByFilters(schoolId, academicYearId, entryType,
                        fromDate, toDate)
                .stream()
                .map(calendarMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkingDayCountResponse getWorkingDayCountResponse(Long schoolId, Long academicYearId,
                                                               LocalDate fromDate, LocalDate toDate) {
        int count = getWorkingDayCount(schoolId, academicYearId, fromDate, toDate);
        return WorkingDayCountResponse.builder()
                .fromDate(fromDate).toDate(toDate).workingDayCount(count)
                .build();
    }

    // =========================================================================
    // Internal APIs
    // =========================================================================

    @Override
    public boolean isWorkingDay(Long schoolId, Long academicYearId, LocalDate date) {
        // Load entries from cache (or DB on miss)
        List<SchoolCalendarEntry> entries = loadFromCacheOrDb(schoolId, academicYearId);

        // Find entry for this specific date
        for (SchoolCalendarEntry entry : entries) {
            if (entry.getEntryDate().equals(date)) {
                return CalendarEntryType.WORKING_DAY.equals(entry.getEntryType());
                // HOLIDAY → false, WORKING_DAY → true
            }
        }

        // No explicit entry: apply weekend rule from school settings
        Set<DayOfWeek> weekends = settingsService.getWeekendDays(schoolId);
        return !weekends.contains(date.getDayOfWeek());
    }

    @Override
    public int getWorkingDayCount(Long schoolId, Long academicYearId,
                                   LocalDate fromDate, LocalDate toDate) {
        int count = 0;
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            if (isWorkingDay(schoolId, academicYearId, current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        return count;
    }

    @Override
    public List<LocalDate> getWorkingDates(Long schoolId, Long academicYearId,
                                            LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> workingDates = new ArrayList<>();
        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            if (isWorkingDay(schoolId, academicYearId, current)) {
                workingDates.add(current);
            }
            current = current.plusDays(1);
        }
        return workingDates;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Validates the academic year belongs to the school and is writable
     * (UPCOMING or ACTIVE, not COMPLETED or CANCELLED).
     * Returns the response so callers can access startDate/endDate.
     */
    private com.attendai.school.academicyear.dto.AcademicYearResponse
            validateWritableYear(Long schoolId, Long academicYearId) {
        var year = academicYearService.findById(schoolId, academicYearId);
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())
                || AcademicYearStatus.CANCELLED.equals(year.getStatus())) {
            throw new ValidationException(
                    "Calendar entries cannot be modified for a "
                    + year.getStatus() + " academic year");
        }
        return year;
    }

    private void validateDateInYearRange(LocalDate date,
                                          LocalDate yearStart, LocalDate yearEnd) {
        if (date.isBefore(yearStart) || date.isAfter(yearEnd)) {
            throw new ValidationException(
                    "Date " + date + " is outside the academic year range ("
                    + yearStart + " to " + yearEnd + ")");
        }
    }

    private void assertNoDuplicate(Long schoolId, Long academicYearId, LocalDate date) {
        if (calendarRepository.existsBySchoolIdAndAcademicYearIdAndEntryDate(
                schoolId, academicYearId, date)) {
            throw new ResourceAlreadyExistsException(
                    "A calendar entry already exists for date " + date
                    + " in academic year " + academicYearId);
        }
    }

    private SchoolCalendarEntry requireEntry(Long schoolId, Long entryId) {
        SchoolCalendarEntry e = calendarRepository.findById(entryId)
                .orElseThrow(() -> new CalendarEntryNotFoundException(entryId));
        if (!e.getSchoolId().equals(schoolId))
            throw new CalendarEntryNotFoundException(entryId);
        return e;
    }

    /**
     * Loads the full calendar entry list for (schoolId, academicYearId)
     * from the in-memory cache, falling back to DB on a cache miss.
     * Results are stored back in the cache on a DB hit.
     */
    private List<SchoolCalendarEntry> loadFromCacheOrDb(Long schoolId, Long academicYearId) {
        return calendarCache.get(schoolId, academicYearId).orElseGet(() -> {
            List<SchoolCalendarEntry> fromDb =
                    calendarRepository.findBySchoolIdAndAcademicYearId(schoolId, academicYearId);
            calendarCache.put(schoolId, academicYearId, fromDb);
            return fromDb;
        });
    }
}
