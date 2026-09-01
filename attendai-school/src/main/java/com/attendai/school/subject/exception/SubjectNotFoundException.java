package com.attendai.school.subject.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class SubjectNotFoundException extends ResourceNotFoundException {
    public SubjectNotFoundException(Long id) {
        super("Subject with id " + id + " was not found");
    }
}
