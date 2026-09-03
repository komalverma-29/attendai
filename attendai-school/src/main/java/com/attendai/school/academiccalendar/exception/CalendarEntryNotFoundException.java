package com.attendai.school.academiccalendar.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class CalendarEntryNotFoundException extends ResourceNotFoundException {
    public CalendarEntryNotFoundException(Long id) {
        super("Calendar entry with id " + id + " was not found");
    }
}
