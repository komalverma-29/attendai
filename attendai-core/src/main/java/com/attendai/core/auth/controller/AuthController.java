package com.attendai.core.auth.controller;

import com.attendai.core.auth.dto.AuthTokenResponse;
import com.attendai.core.auth.dto.LoginRequest;
import com.attendai.core.auth.dto.LogoutRequest;
import com.attendai.core.auth.dto.PasswordResetConfirmRequest;
import com.attendai.core.auth.dto.PasswordResetRequest;
import com.attendai.core.auth.dto.RefreshRequest;
import com.attendai.core.auth.service.AuthService;
import com.attendai.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication endpoints.
 *
 * All endpoints are declared in {@code SecurityConfig} as public (no JWT required).
 * Controllers contain no business logic — all work is delegated to {@link AuthService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/login
     * Authenticates a user and returns access + refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = resolveClientIp(httpRequest);
        AuthTokenResponse tokens = authService.login(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * POST /api/v1/auth/refresh
     * Rotates a refresh token and issues new access + refresh tokens.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        AuthTokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    /**
     * POST /api/v1/auth/logout
     * Revokes the submitted refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Logged out successfully")));
    }

    /**
     * POST /api/v1/auth/password-reset/request
     * Initiates a password reset. Always returns 200 to prevent email enumeration.
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "If the email exists, a reset link has been sent")));
    }

    /**
     * POST /api/v1/auth/password-reset/confirm
     * Confirms a password reset with a valid token and new password.
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Map<String, String>>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {

        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Password reset successfully")));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
