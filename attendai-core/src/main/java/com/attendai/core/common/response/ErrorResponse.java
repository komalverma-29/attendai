package com.attendai.core.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body used inside {@link ApiResponse} when {@code success = false}.
 *
 * {@code fieldErrors} is only populated for validation failures
 * ({@code MethodArgumentNotValidException}, {@code ConstraintViolationException}).
 * It is omitted from the JSON output when null or empty.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** Machine-readable error category code (e.g., "NOT_FOUND", "VALIDATION_FAILED"). */
    private final String code;

    /** Human-readable error message. Must never contain stack traces or internal details. */
    private final String message;

    /** UTC timestamp of when the error occurred. */
    @Builder.Default
    private final Instant timestamp = Instant.now();

    /** Request path that produced the error. */
    private final String path;

    /**
     * Field-level validation errors. Only present for validation failures.
     * Omitted from JSON when null.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldError> fieldErrors;

    // -------------------------------------------------------------------------
    // Nested type
    // -------------------------------------------------------------------------

    /**
     * Represents a single field-level validation failure.
     */
    @Getter
    @Builder
    public static class FieldError {

        /** The field name that failed validation (e.g., "email"). */
        private final String field;

        /** The validation message (e.g., "must be a valid email address"). */
        private final String message;
    }
}
