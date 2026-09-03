package com.attendai.school.timetable.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@Getter
@Builder
public class TimetableEntryResponse {
    private final Long        id;
    private final Long        schoolId;
    private final Long        academicYearId;
    private final Long        sectionId;
    private final Long        timeSlotId;
    private final DayOfWeek   dayOfWeek;
    private final Long        assignmentId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
