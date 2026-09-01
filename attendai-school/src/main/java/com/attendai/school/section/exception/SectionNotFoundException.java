package com.attendai.school.section.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class SectionNotFoundException extends ResourceNotFoundException {
    public SectionNotFoundException(Long id) {
        super("Section with id " + id + " was not found");
    }
}
