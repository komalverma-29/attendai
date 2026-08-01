package com.attendai.core.common.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that every exception class in the hierarchy:
 * - Extends AttendAIException (and therefore RuntimeException)
 * - Carries the correct errorCode
 * - Carries the provided message
 * - Supports cause chaining where declared
 */
class ExceptionHierarchyTest {

    @Test
    void resourceNotFoundException_shouldCarryCorrectCodeAndMessage() {
        var ex = new ResourceNotFoundException("User with id 42 was not found");
        assertThat(ex).isInstanceOf(AttendAIException.class)
                      .isInstanceOf(RuntimeException.class);
        assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("User with id 42 was not found");
    }

    @Test
    void resourceNotFoundException_factoryById_shouldFormatMessage() {
        var ex = ResourceNotFoundException.of("User", 42L);
        assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
        assertThat(ex.getMessage()).isEqualTo("User with id 42 was not found");
    }

    @Test
    void resourceNotFoundException_factoryByField_shouldFormatMessage() {
        var ex = ResourceNotFoundException.of("User", "email", "john@example.com");
        assertThat(ex.getMessage()).isEqualTo("User with email 'john@example.com' was not found");
    }

    @Test
    void resourceAlreadyExistsException_shouldCarryCorrectCodeAndMessage() {
        var ex = new ResourceAlreadyExistsException("User with email 'x@y.com' already exists");
        assertThat(ex.getErrorCode()).isEqualTo("ALREADY_EXISTS");
        assertThat(ex.getMessage()).contains("already exists");
    }

    @Test
    void resourceAlreadyExistsException_factoryByField_shouldFormatMessage() {
        var ex = ResourceAlreadyExistsException.of("User", "email", "x@y.com");
        assertThat(ex.getMessage()).isEqualTo("User with email 'x@y.com' already exists");
    }

    @Test
    void validationException_shouldCarryCorrectCodeAndMessage() {
        var ex = new ValidationException("Cannot deactivate the last administrator");
        assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(ex.getMessage()).isEqualTo("Cannot deactivate the last administrator");
    }

    @Test
    void unauthorizedException_shouldCarryCorrectCodeAndMessage() {
        var ex = new UnauthorizedException("Invalid credentials");
        assertThat(ex.getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(ex.getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void forbiddenException_shouldCarryCorrectCodeAndMessage() {
        var ex = new ForbiddenException("Access denied");
        assertThat(ex.getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(ex.getMessage()).isEqualTo("Access denied");
    }

    @Test
    void externalServiceException_shouldCarryCorrectCodeAndMessage() {
        var ex = new ExternalServiceException("Face recognition engine unavailable");
        assertThat(ex.getErrorCode()).isEqualTo("EXTERNAL_SERVICE_ERROR");
        assertThat(ex.getMessage()).isEqualTo("Face recognition engine unavailable");
    }

    @Test
    void externalServiceException_withCause_shouldChainCause() {
        var cause = new RuntimeException("Connection refused");
        var ex = new ExternalServiceException("External call failed", cause);
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getErrorCode()).isEqualTo("EXTERNAL_SERVICE_ERROR");
    }
}
