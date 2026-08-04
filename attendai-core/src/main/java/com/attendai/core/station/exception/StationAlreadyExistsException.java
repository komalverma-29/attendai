package com.attendai.core.station.exception;

import com.attendai.core.common.exception.ResourceAlreadyExistsException;

public class StationAlreadyExistsException extends ResourceAlreadyExistsException {
    public StationAlreadyExistsException(String name) {
        super("Station with name '" + name + "' already exists");
    }
}
