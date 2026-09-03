package com.attendai.school.timetable.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class TimetableEntryNotFoundException extends ResourceNotFoundException {
    public TimetableEntryNotFoundException(Long id) {
        super("Timetable entry with id " + id + " was not found");
    }
}
