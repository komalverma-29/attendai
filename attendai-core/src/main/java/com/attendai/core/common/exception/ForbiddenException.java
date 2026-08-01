package com.attendai.core.common.exception;

/**
 * Thrown when an authenticated user does not have permission to perform an action.
 * Maps to HTTP 403 Forbidden.
 *
 * In practice, most authorization denials are handled by Spring Security's
 * {@code AccessDeniedException}, which the GlobalExceptionHandler intercepts.
 * This class is used when authorization checks are done manually in service code.
 *
 * Usage example:
 * <pre>
 *   throw new ForbiddenException("You do not have permission to access this resource");
 * </pre>
 */
public class ForbiddenException extends AttendAIException {

    private static final String ERROR_CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(ERROR_CODE, message);
    }
}
