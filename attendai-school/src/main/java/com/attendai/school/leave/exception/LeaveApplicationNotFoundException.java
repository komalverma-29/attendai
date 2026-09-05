package com.attendai.school.leave.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class LeaveApplicationNotFoundException extends ResourceNotFoundException {
    public LeaveApplicationNotFoundException(Long id) {
        super("Leave application with id " + id + " was not found");
    }
}
