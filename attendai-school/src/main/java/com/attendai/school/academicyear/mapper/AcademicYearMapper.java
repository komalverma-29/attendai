package com.attendai.school.academicyear.mapper;

import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.dto.AcademicYearSummaryResponse;
import com.attendai.school.academicyear.entity.SchoolAcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AcademicYearMapper {
    AcademicYearResponse        toResponse(SchoolAcademicYear year);
    AcademicYearSummaryResponse toSummaryResponse(SchoolAcademicYear year);
}
