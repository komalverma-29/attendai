package com.attendai.core.common.exception;

/**
 * Thrown when a call to an external downstream service fails.
 * Maps to HTTP 502 Bad Gateway.
 *
 * Usage examples:
 * - Face recognition engine is unavailable
 * - Email dispatch provider is unreachable
 * - Third-party API call times out
 *
 * <pre>
 *   throw new ExternalServiceException("Face recognition engine is unavailable");
 * </pre>
 */
public class ExternalServiceException extends AttendAIException {

    private static final String ERROR_CODE = "EXTERNAL_SERVICE_ERROR";

    public ExternalServiceException(String message) {
        super(ERROR_CODE, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
    }
}
