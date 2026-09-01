package com.attendai.school.schoolclass.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class ClassNotFoundException extends ResourceNotFoundException {
    public ClassNotFoundException(Long id) {
        super("Class with id " + id + " was not found");
    }
}
