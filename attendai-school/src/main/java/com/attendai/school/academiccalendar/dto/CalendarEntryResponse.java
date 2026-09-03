package com.attendai.school.academiccalendar.dto;

import com.attendai.school.academiccalendar.entity.CalendarEntryType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CalendarEntryResponse {
    private final Long              id;
    private final Long              schoolId;
    private final Long              academicYearId;
    private final LocalDate         entryDate;
    private final CalendarEntryType entryType;
    private final String            name;
    private final String            description;
    private final LocalDateTime     createdAt;
    private final LocalDateTime     updatedAt;
}
