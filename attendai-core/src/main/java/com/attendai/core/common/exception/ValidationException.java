package com.attendai.core.common.exception;

/**
 * Thrown when a business rule validation fails.
 * Maps to HTTP 400 Bad Request.
 *
 * This exception is for service-layer business rule violations — not for
 * Bean Validation (@Valid) failures, which are handled automatically by
 * {@link com.attendai.core.common.handler.GlobalExceptionHandler}.
 *
 * Usage example:
 * <pre>
 *   throw new ValidationException("Cannot deactivate the last active administrator of a school");
 * </pre>
 */
public class ValidationException extends AttendAIException {

    private static final String ERROR_CODE = "VALIDATION_FAILED";

    public ValidationException(String message) {
        super(ERROR_CODE, message);
    }
}
