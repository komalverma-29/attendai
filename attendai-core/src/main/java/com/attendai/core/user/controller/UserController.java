package com.attendai.core.user.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.user.dto.ChangePasswordRequest;
import com.attendai.core.user.dto.ChangeStatusRequest;
import com.attendai.core.user.dto.CreateUserRequest;
import com.attendai.core.user.dto.UpdateUserRequest;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.dto.UserSummaryResponse;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for user account management.
 * Base path: /api/v1/core/users
 *
 * No business logic lives here — all work delegates to {@link UserService}.
 */
@RestController
@RequestMapping("/api/v1/core/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** POST /api/v1/core/users — Create a new user account. */
    @PostMapping
    @PreAuthorize("hasAuthority('CORE_USER_CREATE')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.createUser(request)));
    }

    /** GET /api/v1/core/users/{id} — Retrieve a user by ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_USER_READ')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.findById(id)));
    }

    /** GET /api/v1/core/users — List users with optional filters. */
    @GetMapping
    @PreAuthorize("hasAuthority('CORE_USER_READ')")
    public ResponseEntity<PageResponse<UserSummaryResponse>> listUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(
                PageResponse.of(userService.listUsers(status, search, pageParams.toPageable())));
    }

    /** PUT /api/v1/core/users/{id} — Update a user account. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }

    /** PATCH /api/v1/core/users/{id}/status — Change user account status. */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CORE_USER_UPDATE')")
    public ResponseEntity<ApiResponse<UserResponse>> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.changeStatus(id, request)));
    }

    /**
     * POST /api/v1/core/users/{id}/change-password — Change own password.
     *
     * An authenticated user can change their own password. An admin with
     * CORE_USER_UPDATE can change any user's password (currentPassword is still required
     * on this endpoint — admin resets go through core-auth password-reset flow).
     */
    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangePasswordRequest request) {

        Long currentUserId = SecurityContextUtils.getCurrentUserId().orElse(null);
        boolean isSelf = id.equals(currentUserId);
        boolean isAdmin = SecurityContextUtils.hasAuthority("CORE_USER_UPDATE");

        if (!isSelf && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(
                            com.attendai.core.common.response.ErrorResponse.builder()
                                    .code("FORBIDDEN")
                                    .message("You can only change your own password")
                                    .build()));
        }

        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Password changed successfully")));
    }

    /** DELETE /api/v1/core/users/{id} — Soft-delete a user account. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CORE_USER_DELETE')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
