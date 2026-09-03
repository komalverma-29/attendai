package com.attendai.school.attendancerules.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class AttendanceRuleSetNotFoundException extends ResourceNotFoundException {
    public AttendanceRuleSetNotFoundException(Long schoolId, Long academicYearId) {
        super("No attendance rule set found for school " + schoolId
              + " and academic year " + academicYearId);
    }
    public AttendanceRuleSetNotFoundException(Long id) {
        super("Attendance rule set with id " + id + " was not found");
    }
}
