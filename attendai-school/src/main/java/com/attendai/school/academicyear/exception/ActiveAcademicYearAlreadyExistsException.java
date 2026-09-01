package com.attendai.school.academicyear.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class ActiveAcademicYearAlreadyExistsException extends ResourceAlreadyExistsException {
    public ActiveAcademicYearAlreadyExistsException(Long schoolId) {
        super("School " + schoolId + " already has an ACTIVE academic year");
    }
}
