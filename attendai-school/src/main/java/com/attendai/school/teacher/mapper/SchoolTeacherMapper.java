package com.attendai.school.teacher.mapper;

import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.dto.TeacherSummaryResponse;
import com.attendai.school.teacher.entity.SchoolTeacher;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SchoolTeacherMapper {
    TeacherResponse        toResponse(SchoolTeacher teacher);
    TeacherSummaryResponse toSummaryResponse(SchoolTeacher teacher);
}
