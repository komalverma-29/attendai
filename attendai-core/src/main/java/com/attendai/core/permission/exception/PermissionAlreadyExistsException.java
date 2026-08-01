package com.attendai.core.permission.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class PermissionAlreadyExistsException extends ResourceAlreadyExistsException {
    public PermissionAlreadyExistsException(String code) {
        super("Permission with code '" + code + "' already exists");
    }
}
