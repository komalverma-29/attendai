package com.attendai.school.timetable.dto;

import com.attendai.school.timetable.entity.TimeSlotType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class TimeSlotResponse {
    private final Long         id;
    private final Long         schoolId;
    private final String       name;
    private final LocalTime    startTime;
    private final LocalTime    endTime;
    private final int          slotOrder;
    private final TimeSlotType slotType;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
