package com.attendai.school.leave.mapper;

import com.attendai.school.leave.dto.LeaveApplicationResponse;
import com.attendai.school.leave.dto.LeaveApplicationSummaryResponse;
import com.attendai.school.leave.entity.LeaveApplication;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LeaveApplicationMapper {
    LeaveApplicationResponse        toResponse(LeaveApplication leave);
    LeaveApplicationSummaryResponse toSummaryResponse(LeaveApplication leave);
}
