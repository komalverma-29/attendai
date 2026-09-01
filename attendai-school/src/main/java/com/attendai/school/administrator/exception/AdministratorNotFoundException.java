package com.attendai.school.administrator.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class AdministratorNotFoundException extends ResourceNotFoundException {
    public AdministratorNotFoundException(Long id) {
        super("Administrator with id " + id + " was not found");
    }
}
