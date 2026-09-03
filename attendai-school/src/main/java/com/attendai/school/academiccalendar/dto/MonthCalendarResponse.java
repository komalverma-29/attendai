package com.attendai.school.academiccalendar.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Day-by-day calendar status for a given month within an academic year.
 *
 * <p>Each {@link DayStatus} entry shows whether the date is a WORKING_DAY,
 * HOLIDAY, or WEEKEND (derived from school settings), along with the
 * calendar entry name if one exists.
 */
@Getter
@Builder
public class MonthCalendarResponse {

    private final int          year;
    private final int          month;
    private final List<DayStatus> days;

    @Getter
    @Builder
    public static class DayStatus {
        private final LocalDate  date;
        private final DayOfWeek  dayOfWeek;
        /**
         * One of: "WORKING_DAY", "HOLIDAY", "WEEKEND".
         * WORKING_DAY = normal or explicitly declared working day.
         * HOLIDAY     = declared holiday entry.
         * WEEKEND     = day falls on a school weekend (no explicit entry).
         */
        private final String     status;
        /** Name of the calendar entry if one exists, otherwise null. */
        private final String     entryName;
    }
}
