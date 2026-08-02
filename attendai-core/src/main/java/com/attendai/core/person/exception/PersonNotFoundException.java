package com.attendai.core.person.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class PersonNotFoundException extends ResourceNotFoundException {

    public PersonNotFoundException(Long id) {
        super("Person with id " + id + " was not found");
    }
}
