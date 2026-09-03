package com.attendai.school.teacherassignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeacherAssignmentRequest {

    @NotNull(message = "Section id is required")
    private Long sectionId;

    @NotNull(message = "Subject id is required")
    private Long subjectId;

    @NotNull(message = "Teacher id is required")
    private Long teacherId;

    /** Whether this teacher is the class teacher for the section. Default false. */
    private boolean classTeacher = false;

    private String notes;
}
