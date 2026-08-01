package com.attendai.core.user.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(Long id) {
        super("User with id " + id + " was not found");
    }

    public UserNotFoundException(String field, String value) {
        super("User with " + field + " '" + value + "' was not found");
    }
}
