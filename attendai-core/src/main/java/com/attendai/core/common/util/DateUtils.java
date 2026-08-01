package com.attendai.core.common.util;

import com.attendai.core.common.constants.AttendAIConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and time utility methods used across the AttendAI platform.
 *
 * All methods are static. This class is not instantiable.
 */
public final class DateUtils {

    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern(AttendAIConstants.LOG_DATETIME_PATTERN);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(AttendAIConstants.DATE_PATTERN);

    private DateUtils() {
        // Utility class
    }

    /**
     * Formats a {@link LocalDateTime} for log output.
     *
     * @param dateTime the datetime to format; may be null
     * @return formatted string, or "null" if input is null
     */
    public static String formatForLog(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "null";
        }
        return dateTime.format(LOG_FORMATTER);
    }

    /**
     * Formats a {@link LocalDate} as an ISO date string (yyyy-MM-dd).
     *
     * @param date the date to format; may be null
     * @return formatted string, or "null" if input is null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "null";
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * Returns {@code true} if the given date is today or in the past.
     *
     * @param date the date to check
     * @return {@code true} if the date is not in the future
     */
    public static boolean isPastOrToday(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    /**
     * Returns {@code true} if the given datetime is within the allowed future window
     * defined by {@link AttendAIConstants#MAX_FUTURE_EVENT_SECONDS}.
     *
     * @param eventTime the event timestamp to check
     * @return {@code true} if the event time is acceptable
     */
    public static boolean isWithinAllowedFutureWindow(LocalDateTime eventTime) {
        if (eventTime == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().plusSeconds(AttendAIConstants.MAX_FUTURE_EVENT_SECONDS);
        return !eventTime.isAfter(cutoff);
    }

    /**
     * Returns {@code true} if the given datetime is within the allowed past window
     * defined by {@link AttendAIConstants#MAX_PAST_EVENT_HOURS}.
     *
     * @param eventTime the event timestamp to check
     * @return {@code true} if the event time is not too far in the past
     */
    public static boolean isWithinAllowedPastWindow(LocalDateTime eventTime) {
        if (eventTime == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusHours(AttendAIConstants.MAX_PAST_EVENT_HOURS);
        return !eventTime.isBefore(cutoff);
    }
}
