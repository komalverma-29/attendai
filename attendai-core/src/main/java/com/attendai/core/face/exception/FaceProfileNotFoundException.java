package com.attendai.core.face.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class FaceProfileNotFoundException extends ResourceNotFoundException {
    public FaceProfileNotFoundException(Long id) {
        super("Face profile with id " + id + " was not found");
    }
}
