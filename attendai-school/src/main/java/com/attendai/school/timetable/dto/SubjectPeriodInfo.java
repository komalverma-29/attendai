package com.attendai.school.timetable.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Lightweight projection used by the internal API
 * {@code TimetableService.getSubjectsForSectionOnDay}.
 *
 * <p>Consumed by {@code school-daily-attendance} to determine which subjects
 * require attendance marking for a given section on a given day.
 */
@Getter
@Builder
public class SubjectPeriodInfo {
    private final Long subjectId;
    private final Long assignmentId;
    private final Long timeSlotId;
}
