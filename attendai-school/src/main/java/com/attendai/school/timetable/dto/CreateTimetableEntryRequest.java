package com.attendai.school.timetable.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;

@Getter
@Setter
public class CreateTimetableEntryRequest {

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Time slot id is required")
    private Long timeSlotId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Assignment id is required")
    private Long assignmentId;
}
