package com.attendai.school.attendancecorrections.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class CorrectionRequestNotFoundException extends ResourceNotFoundException {
    public CorrectionRequestNotFoundException(Long id) {
        super("Correction request with id " + id + " was not found");
    }
}
