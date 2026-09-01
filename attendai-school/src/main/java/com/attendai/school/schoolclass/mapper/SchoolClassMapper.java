package com.attendai.school.schoolclass.mapper;

import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.dto.ClassSummaryResponse;
import com.attendai.school.schoolclass.entity.SchoolClass;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolClassMapper {
    ClassResponse        toResponse(SchoolClass schoolClass);
    ClassSummaryResponse toSummaryResponse(SchoolClass schoolClass);
}
