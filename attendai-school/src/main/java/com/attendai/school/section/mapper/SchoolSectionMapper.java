package com.attendai.school.section.mapper;

import com.attendai.school.section.dto.SectionEnrollmentResponse;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.entity.SchoolSection;
import com.attendai.school.section.entity.SectionEnrollment;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolSectionMapper {
    SectionResponse          toResponse(SchoolSection section);
    SectionEnrollmentResponse toEnrollmentResponse(SectionEnrollment enrollment);
}
