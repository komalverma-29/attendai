package com.attendai.school.school.mapper;

import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.dto.SchoolSummaryResponse;
import com.attendai.school.school.entity.School;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolMapper {
    SchoolResponse        toResponse(School school);
    SchoolSummaryResponse toSummaryResponse(School school);
}
