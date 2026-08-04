package com.attendai.core.station.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class StationNotFoundException extends ResourceNotFoundException {
    public StationNotFoundException(Long id) {
        super("Station with id " + id + " was not found");
    }
}
