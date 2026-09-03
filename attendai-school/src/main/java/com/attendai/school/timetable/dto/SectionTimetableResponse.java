package com.attendai.school.timetable.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

/**
 * Full weekly timetable for a section.
 *
 * <p>The {@code schedule} map is keyed by day of week and contains an ordered
 * list of timetable entries for that day (ordered by time slot's slotOrder).
 */
@Getter
@Builder
public class SectionTimetableResponse {
    private final Long                              sectionId;
    private final Long                              academicYearId;
    private final Map<DayOfWeek, List<TimetableEntryResponse>> schedule;
}
