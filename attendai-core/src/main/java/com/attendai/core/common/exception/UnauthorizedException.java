package com.attendai.core.common.exception;

/**
 * Thrown when authentication fails or is missing.
 * Maps to HTTP 401 Unauthorized.
 *
 * Do NOT reveal whether the failure was due to a wrong password or
 * an unknown user — always use a generic message to prevent user enumeration.
 *
 * Usage example:
 * <pre>
 *   throw new UnauthorizedException("Invalid credentials");
 * </pre>
 */
public class UnauthorizedException extends AttendAIException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(ERROR_CODE, message);
    }
}
