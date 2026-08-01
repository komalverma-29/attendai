package com.attendai.core.role.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class RoleAlreadyExistsException extends ResourceAlreadyExistsException {
    public RoleAlreadyExistsException(String code) {
        super("Role with code '" + code + "' already exists");
    }
}
