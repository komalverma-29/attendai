package com.attendai.school.academiccalendar.entity;

/**
 * Type of a school calendar entry.
 *
 * <p>Only exceptions to the default schedule are stored:
 * <ul>
 *   <li>{@link #HOLIDAY} — a normally-working day declared as a holiday</li>
 *   <li>{@link #WORKING_DAY} — a normally-non-working day (weekend) declared as a working day</li>
 * </ul>
 *
 * Regular weekday working days have NO calendar entry.
 */
public enum CalendarEntryType {
    HOLIDAY,
    WORKING_DAY
}
