package com.attendai.school.section.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class SectionEnrollmentNotFoundException extends ResourceNotFoundException {
    public SectionEnrollmentNotFoundException(Long studentId, Long sectionId) {
        super("Student " + studentId + " is not enrolled in section " + sectionId);
    }
}
