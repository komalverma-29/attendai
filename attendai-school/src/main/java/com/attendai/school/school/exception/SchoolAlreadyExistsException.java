package com.attendai.school.school.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class SchoolAlreadyExistsException extends ResourceAlreadyExistsException {
    public SchoolAlreadyExistsException(String field, String value) {
        super("School with " + field + " '" + value + "' already exists");
    }
}
