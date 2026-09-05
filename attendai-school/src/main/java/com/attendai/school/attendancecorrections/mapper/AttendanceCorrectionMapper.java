package com.attendai.school.attendancecorrections.mapper;

import com.attendai.school.attendancecorrections.dto.CorrectionRequestResponse;
import com.attendai.school.attendancecorrections.dto.CorrectionSummaryResponse;
import com.attendai.school.attendancecorrections.entity.AttendanceCorrectionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceCorrectionMapper {
    CorrectionRequestResponse toResponse(AttendanceCorrectionRequest c);
    CorrectionSummaryResponse toSummaryResponse(AttendanceCorrectionRequest c);
}
