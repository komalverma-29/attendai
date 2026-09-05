package com.attendai.school.dailyattendance.mapper;

import com.attendai.school.dailyattendance.dto.DailyAttendanceRecordResponse;
import com.attendai.school.dailyattendance.entity.DailyAttendanceRecord;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DailyAttendanceMapper {
    DailyAttendanceRecordResponse toResponse(DailyAttendanceRecord record);
}
