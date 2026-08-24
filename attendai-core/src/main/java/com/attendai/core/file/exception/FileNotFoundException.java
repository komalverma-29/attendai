package com.attendai.core.file.exception;

import com.attendai.core.common.exception.ResourceNotFoundException;

public class FileNotFoundException extends ResourceNotFoundException {
    public FileNotFoundException(Long id) {
        super("File with id " + id + " was not found");
    }
}
