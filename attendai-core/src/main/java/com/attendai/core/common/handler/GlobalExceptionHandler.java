package com.attendai.core.common.handler;

import com.attendai.core.common.exception.AttendAIException;
import com.attendai.core.common.exception.ExternalServiceException;
import com.attendai.core.common.exception.ForbiddenException;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for all AttendAI REST controllers.
 *
 * <p>Intercepts every exception type defined in the exception hierarchy and
 * every Spring / Security exception, converts them to a structured
 * {@link ApiResponse} with an {@link ErrorResponse} body, and returns the
 * appropriate HTTP status code.
 *
 * <p>Security rules enforced here:
 * <ul>
 *   <li>Stack traces are NEVER included in any response.</li>
 *   <li>Internal class names, SQL details, or infrastructure information are
 *       never included in responses.</li>
 *   <li>The 500 fallback logs the full exception internally (ERROR level) but
 *       returns only a generic "An unexpected error occurred" message.</li>
 *   <li>4xx errors are logged at WARN level; 5xx errors at ERROR level.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // AttendAI domain exceptions
    // -------------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("[HTTP 404] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyExists(
            ResourceAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("[HTTP 409] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            ValidationException ex, HttpServletRequest request) {
        log.warn("[HTTP 400] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("[HTTP 401] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        log.warn("[HTTP 403] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(
            ExternalServiceException ex, HttpServletRequest request) {
        log.error("[HTTP 502] {} - {} | path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(buildError(ex, request.getRequestURI())));
    }

    // -------------------------------------------------------------------------
    // Spring / Jakarta Bean Validation exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles @Valid failures on @RequestBody parameters.
     * Produces a fieldErrors array in the response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("[HTTP 400] VALIDATION_FAILED - {} field errors | path={}", fieldErrors.size(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * Handles @Validated failures on @RequestParam / @PathVariable parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(extractFieldName(cv))
                        .message(cv.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("[HTTP 400] VALIDATION_FAILED - constraint violations | path={}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * Handles malformed or unreadable request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("[HTTP 400] MALFORMED_REQUEST | path={}", request.getRequestURI());
        ErrorResponse error = ErrorResponse.builder()
                .code("MALFORMED_REQUEST")
                .message("Request body is malformed or missing")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    /**
     * Handles missing required @RequestParam values.
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParam(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpServletRequest request) {
        log.warn("[HTTP 400] MISSING_PARAMETER - {} | path={}", ex.getParameterName(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Required parameter '" + ex.getParameterName() + "' is missing")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(error));
    }

    // -------------------------------------------------------------------------
    // Spring Security exceptions
    // -------------------------------------------------------------------------

    /**
     * Handles Spring Security authorization failures (403).
     * This is only reached if the exception propagates past the security filter chain.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[HTTP 403] FORBIDDEN - Access denied | path={}", request.getRequestURI());
        ErrorResponse error = ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("You do not have permission to access this resource")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    /**
     * Handles Spring Security authentication failures (401).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("[HTTP 401] UNAUTHORIZED - Authentication failed | path={}", request.getRequestURI());
        ErrorResponse error = ErrorResponse.builder()
                .code("UNAUTHORIZED")
                .message("Authentication is required to access this resource")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error));
    }

    /**
     * Handles 404 for unmapped routes (Spring 6+).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("[HTTP 404] NOT_FOUND - No handler for path | path={}", request.getRequestURI());
        ErrorResponse error = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("The requested path was not found")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(error));
    }

    // -------------------------------------------------------------------------
    // Fallback — catches all unhandled exceptions
    // -------------------------------------------------------------------------

    /**
     * Fallback handler for any unhandled exception.
     *
     * <p>Logs the full exception at ERROR level for internal diagnosis but
     * returns only a generic message to the client. Stack traces and internal
     * details must never reach the response body.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("[HTTP 500] INTERNAL_ERROR - Unexpected exception | path={}", request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ErrorResponse buildError(AttendAIException ex, String path) {
        return ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .path(path)
                .build();
    }

    private String extractFieldName(ConstraintViolation<?> cv) {
        String propertyPath = cv.getPropertyPath().toString();
        // Path is typically "methodName.paramName.fieldName" — take the last segment
        String[] parts = propertyPath.split("\\.");
        return parts[parts.length - 1];
    }
}
