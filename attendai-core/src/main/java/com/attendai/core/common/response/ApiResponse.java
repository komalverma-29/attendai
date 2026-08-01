package com.attendai.core.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * Standard API response envelope used by all AttendAI REST endpoints.
 *
 * <p>Success response:
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": { ... }
 * }
 * }</pre>
 *
 * <p>Error response:
 * <pre>{@code
 * {
 *   "success": false,
 *   "error": { "code": "NOT_FOUND", "message": "...", "timestamp": "..." }
 * }
 * }</pre>
 *
 * Use the static factory methods {@link #success(Object)} and {@link #error(ErrorResponse)}
 * rather than constructing instances directly.
 *
 * @param <T> the type of the wrapped data payload
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private ApiResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * Creates a successful response wrapping the given data payload.
     *
     * @param data the response payload; may be null for 204-style responses
     * @param <T>  the payload type
     * @return a success-flagged {@link ApiResponse}
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Creates a successful response with no data payload (e.g., for 204 No Content).
     *
     * @param <T> the payload type
     * @return a success-flagged {@link ApiResponse} with null data
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * Creates an error response wrapping the given error details.
     *
     * @param error the error details
     * @param <T>   the payload type (data will be null)
     * @return an error-flagged {@link ApiResponse}
     */
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
