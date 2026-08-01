package com.attendai.core.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ApiResponse envelope factory methods and PageResponse construction.
 */
class ApiResponseTest {

    // -------------------------------------------------------------------------
    // ApiResponse
    // -------------------------------------------------------------------------

    @Test
    void success_withData_shouldSetSuccessTrueAndPopulateData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
    }

    @Test
    void success_withoutData_shouldSetSuccessTrueAndNullData() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void error_shouldSetSuccessFalseAndPopulateError() {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("Resource not found")
                .path("/api/v1/test")
                .build();

        ApiResponse<Void> response = ApiResponse.error(errorResponse);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getError().getMessage()).isEqualTo("Resource not found");
        assertThat(response.getError().getPath()).isEqualTo("/api/v1/test");
    }

    // -------------------------------------------------------------------------
    // ErrorResponse
    // -------------------------------------------------------------------------

    @Test
    void errorResponse_withFieldErrors_shouldPopulateFieldErrors() {
        List<ErrorResponse.FieldError> fieldErrors = List.of(
                ErrorResponse.FieldError.builder().field("email").message("must be a valid email").build(),
                ErrorResponse.FieldError.builder().field("firstName").message("must not be blank").build()
        );

        ErrorResponse error = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("Request validation failed")
                .path("/api/v1/users")
                .fieldErrors(fieldErrors)
                .build();

        assertThat(error.getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(error.getFieldErrors()).hasSize(2);
        assertThat(error.getFieldErrors().get(0).getField()).isEqualTo("email");
        assertThat(error.getFieldErrors().get(1).getField()).isEqualTo("firstName");
    }

    @Test
    void errorResponse_timestampShouldBeAutoPopulated() {
        ErrorResponse error = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("Not found")
                .build();

        assertThat(error.getTimestamp()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // PageResponse
    // -------------------------------------------------------------------------

    @Test
    void pageResponse_of_shouldMapSpringPageCorrectly() {
        // Build a Spring Page manually using PageImpl
        var content = List.of("a", "b", "c");
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        var page = new org.springframework.data.domain.PageImpl<>(content, pageable, 3L);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsExactly("a", "b", "c");
        assertThat(response.getPagination().getPage()).isZero();
        assertThat(response.getPagination().getSize()).isEqualTo(20);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(3L);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(1);
        assertThat(response.getPagination().isFirst()).isTrue();
        assertThat(response.getPagination().isLast()).isTrue();
    }

    @Test
    void pageResponse_multiPage_shouldSetFirstAndLastCorrectly() {
        var content = List.of("a", "b");
        var pageable = org.springframework.data.domain.PageRequest.of(1, 2);
        var page = new org.springframework.data.domain.PageImpl<>(content, pageable, 10L);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.getPagination().getPage()).isEqualTo(1);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(5);
        assertThat(response.getPagination().isFirst()).isFalse();
        assertThat(response.getPagination().isLast()).isFalse();
    }
}
