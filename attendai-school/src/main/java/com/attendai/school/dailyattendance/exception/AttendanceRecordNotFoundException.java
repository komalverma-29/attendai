package com.attendai.school.dailyattendance.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class AttendanceRecordNotFoundException extends ResourceNotFoundException {
    public AttendanceRecordNotFoundException(Long id) {
        super("Attendance record with id " + id + " was not found");
    }
}
