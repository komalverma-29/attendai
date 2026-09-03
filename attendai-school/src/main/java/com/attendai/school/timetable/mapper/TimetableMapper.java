package com.attendai.school.timetable.mapper;

import com.attendai.school.timetable.dto.TimeSlotResponse;
import com.attendai.school.timetable.dto.TimetableEntryResponse;
import com.attendai.school.timetable.entity.SchoolTimeSlot;
import com.attendai.school.timetable.entity.TimetableEntry;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimetableMapper {
    TimeSlotResponse       toTimeSlotResponse(SchoolTimeSlot slot);
    TimetableEntryResponse toEntryResponse(TimetableEntry entry);
}
