package com.attendai.school.student.mapper;

import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.dto.StudentSummaryResponse;
import com.attendai.school.student.entity.SchoolStudent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolStudentMapper {
    StudentResponse        toResponse(SchoolStudent student);
    StudentSummaryResponse toSummaryResponse(SchoolStudent student);
}
