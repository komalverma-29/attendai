package com.attendai.core.permission.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class PermissionNotFoundException extends ResourceNotFoundException {
    public PermissionNotFoundException(Long id) {
        super("Permission with id " + id + " was not found");
    }
}
