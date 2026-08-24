package com.attendai.core.file.exception;

import com.attendai.core.common.exception.ValidationException;

/**
 * Thrown when an uploaded file's content type is not in the configured allowlist.
 * Maps to HTTP 400 Bad Request.
 */
public class FileTypeNotAllowedException extends ValidationException {

    public FileTypeNotAllowedException(String contentType) {
        super("File type '" + contentType + "' is not allowed");
    }
}
