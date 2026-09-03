package com.attendai.school.academiccalendar.mapper;

import com.attendai.school.academiccalendar.dto.CalendarEntryResponse;
import com.attendai.school.academiccalendar.entity.SchoolCalendarEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AcademicCalendarMapper {
    CalendarEntryResponse toResponse(SchoolCalendarEntry entry);
}
