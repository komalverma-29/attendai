package com.attendai.school.teacherassignment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeacherAssignmentRequest {

    /** Change the assigned teacher. Must reference an ACTIVE teacher in the same school. */
    private Long teacherId;

    /** Update the class-teacher flag. */
    private Boolean classTeacher;

    private String notes;
}
