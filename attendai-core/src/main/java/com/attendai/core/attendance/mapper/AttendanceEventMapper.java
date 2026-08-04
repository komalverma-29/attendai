package com.attendai.core.attendance.mapper;

import com.attendai.core.attendance.dto.AttendanceEventResponse;
import com.attendai.core.attendance.dto.AttendanceEventSummaryResponse;
import com.attendai.core.attendance.entity.AttendanceEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceEventMapper {
    AttendanceEventResponse       toResponse(AttendanceEvent event);
    AttendanceEventSummaryResponse toSummaryResponse(AttendanceEvent event);
}
