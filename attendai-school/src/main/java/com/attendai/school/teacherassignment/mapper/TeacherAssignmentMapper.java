package com.attendai.school.teacherassignment.mapper;

import com.attendai.school.teacherassignment.dto.TeacherAssignmentResponse;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentSummaryResponse;
import com.attendai.school.teacherassignment.entity.TeacherAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeacherAssignmentMapper {

    @Mapping(target = "classTeacher", source = "classTeacher")
    TeacherAssignmentResponse toResponse(TeacherAssignment assignment);

    @Mapping(target = "classTeacher", source = "classTeacher")
    TeacherAssignmentSummaryResponse toSummaryResponse(TeacherAssignment assignment);
}
