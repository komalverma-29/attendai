package com.attendai.school.school.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class SchoolNotFoundException extends ResourceNotFoundException {
    public SchoolNotFoundException(Long id) {
        super("School with id " + id + " was not found");
    }
}
