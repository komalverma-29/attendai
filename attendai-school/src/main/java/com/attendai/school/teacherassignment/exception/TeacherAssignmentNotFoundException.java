package com.attendai.school.teacherassignment.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class TeacherAssignmentNotFoundException extends ResourceNotFoundException {
    public TeacherAssignmentNotFoundException(Long id) {
        super("Teacher assignment with id " + id + " was not found");
    }
}
