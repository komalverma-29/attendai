package com.attendai.school.student.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class StudentNotFoundException extends ResourceNotFoundException {
    public StudentNotFoundException(Long id) {
        super("Student with id " + id + " was not found");
    }
}
