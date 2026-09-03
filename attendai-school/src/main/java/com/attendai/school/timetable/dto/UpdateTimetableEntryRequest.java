package com.attendai.school.timetable.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTimetableEntryRequest {

    /** New assignment to use for this slot. Must be ACTIVE and belong to the same school. */
    private Long assignmentId;
}
