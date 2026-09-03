package com.attendai.school.teacherassignment.dto;

import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherAssignmentSummaryResponse {
    private final Long             id;
    private final Long             sectionId;
    private final Long             subjectId;
    private final Long             teacherId;
    private final boolean          classTeacher;
    private final AssignmentStatus status;
}
