package com.attendai.school.teacher.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class TeacherNotFoundException extends ResourceNotFoundException {
    public TeacherNotFoundException(Long id) {
        super("Teacher with id " + id + " was not found");
    }
}
