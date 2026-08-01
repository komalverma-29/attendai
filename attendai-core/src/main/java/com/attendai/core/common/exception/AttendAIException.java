package com.attendai.core.common.exception;

import lombok.Getter;

/**
 * Root exception for all AttendAI platform exceptions.
 *
 * Every module-specific exception must extend one of the concrete subtypes
 * defined in this package. This class is not thrown directly.
 *
 * Carries an {@code errorCode} string used in API error responses (e.g., "NOT_FOUND")
 * so clients can programmatically react to error categories without parsing messages.
 */
@Getter
public abstract class AttendAIException extends RuntimeException {

    private final String errorCode;

    protected AttendAIException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AttendAIException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
