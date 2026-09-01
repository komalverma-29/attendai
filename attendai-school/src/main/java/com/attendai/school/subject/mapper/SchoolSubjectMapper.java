package com.attendai.school.subject.mapper;

import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.dto.SubjectSummaryResponse;
import com.attendai.school.subject.entity.SchoolSubject;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolSubjectMapper {
    SubjectResponse        toResponse(SchoolSubject subject);
    SubjectSummaryResponse toSummaryResponse(SchoolSubject subject);
}
