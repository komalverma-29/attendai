package com.attendai.core.common.handler;

import com.attendai.core.common.exception.ExternalServiceException;
import com.attendai.core.common.exception.ForbiddenException;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 * Each test verifies that the correct HTTP status and error code are returned,
 * and that no stack trace leaks into the response.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    void handleNotFound_shouldReturn404WithNotFoundCode() {
        var ex = new ResourceNotFoundException("User not found");
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleAlreadyExists_shouldReturn409WithAlreadyExistsCode() {
        var ex = new ResourceAlreadyExistsException("Email already exists");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAlreadyExists(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError().getCode()).isEqualTo("ALREADY_EXISTS");
    }

    @Test
    void handleValidation_shouldReturn400WithValidationFailedCode() {
        var ex = new ValidationException("Cannot delete last admin");
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void handleUnauthorized_shouldReturn401WithUnauthorizedCode() {
        var ex = new UnauthorizedException("Invalid credentials");
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError().getCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleForbidden_shouldReturn403WithForbiddenCode() {
        var ex = new ForbiddenException("Access denied");
        ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleExternalService_shouldReturn502WithExternalServiceErrorCode() {
        var ex = new ExternalServiceException("Recognition engine down");
        ResponseEntity<ApiResponse<Void>> response = handler.handleExternalService(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody().getError().getCode()).isEqualTo("EXTERNAL_SERVICE_ERROR");
    }

    @Test
    void handleAccessDenied_shouldReturn403WithForbiddenCode() {
        var ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("FORBIDDEN");
    }

    @Test
    void handleAuthentication_shouldReturn401WithUnauthorizedCode() {
        var ex = new AuthenticationException("Bad token") {};
        ResponseEntity<ApiResponse<Void>> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError().getCode()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void handleUnexpected_shouldReturn500WithGenericMessage() {
        var ex = new RuntimeException("Database is on fire");
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_ERROR");
        // Generic message — must not expose internal detail
        assertThat(response.getBody().getError().getMessage())
                .doesNotContain("Database is on fire")
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleUnexpected_responseBody_shouldNeverContainStackTrace() {
        var ex = new NullPointerException("null pointer at line 42");
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(ex, request);

        String body = response.getBody().getError().getMessage();
        assertThat(body).doesNotContain("NullPointerException");
        assertThat(body).doesNotContain("line 42");
        assertThat(body).doesNotContain("at com.");
    }
}
