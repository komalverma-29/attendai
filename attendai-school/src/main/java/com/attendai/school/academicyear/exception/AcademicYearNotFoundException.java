package com.attendai.school.academicyear.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class AcademicYearNotFoundException extends ResourceNotFoundException {
    public AcademicYearNotFoundException(Long id) {
        super("Academic year with id " + id + " was not found");
    }
    public AcademicYearNotFoundException(String message) {
        super(message);
    }
}
