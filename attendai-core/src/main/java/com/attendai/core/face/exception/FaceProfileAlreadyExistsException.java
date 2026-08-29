package com.attendai.core.face.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class FaceProfileAlreadyExistsException extends ResourceAlreadyExistsException {
    public FaceProfileAlreadyExistsException(Long personId) {
        super("Person with id " + personId + " already has an active face profile");
    }
}
