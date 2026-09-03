package com.attendai.school.teacherassignment.dto;

import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherAssignmentResponse {
    private final Long             id;
    private final Long             schoolId;
    private final Long             academicYearId;
    private final Long             sectionId;
    private final Long             subjectId;
    private final Long             teacherId;
    private final boolean          classTeacher;
    private final AssignmentStatus status;
    private final String           notes;
    private final LocalDateTime    createdAt;
    private final LocalDateTime    updatedAt;
}
