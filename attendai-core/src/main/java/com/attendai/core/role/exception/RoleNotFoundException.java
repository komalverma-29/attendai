package com.attendai.core.role.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class RoleNotFoundException extends ResourceNotFoundException {
    public RoleNotFoundException(Long id) {
        super("Role with id " + id + " was not found");
    }
    public RoleNotFoundException(String field, String value) {
        super("Role with " + field + " '" + value + "' was not found");
    }
}
