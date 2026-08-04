package com.attendai.core.attendance.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class AttendanceEventNotFoundException extends ResourceNotFoundException {
    public AttendanceEventNotFoundException(Long id) {
        super("Attendance event with id " + id + " was not found");
    }
}
