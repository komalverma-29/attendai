package com.attendai.core.auth.service;

import com.attendai.core.auth.dto.AuthTokenResponse;
import com.attendai.core.auth.dto.LoginRequest;
import com.attendai.core.auth.dto.LogoutRequest;
import com.attendai.core.auth.dto.PasswordResetConfirmRequest;
import com.attendai.core.auth.dto.PasswordResetRequest;
import com.attendai.core.auth.dto.RefreshRequest;

/**
 * Core authentication service.
 * All authentication flows route through this interface.
 */
public interface AuthService {

    /**
     * Authenticates a user with email and password.
     * Returns access and refresh tokens on success.
     * Throws {@link com.attendai.core.common.exception.UnauthorizedException} on failure.
     * Never reveals whether the email exists.
     */
    AuthTokenResponse login(LoginRequest request, String ipAddress);

    /**
     * Rotates a refresh token.
     * Revokes the submitted token and issues a new access+refresh pair.
     * Throws {@link com.attendai.core.common.exception.UnauthorizedException} if invalid/revoked/expired.
     */
    AuthTokenResponse refresh(RefreshRequest request);

    /**
     * Revokes the submitted refresh token.
     * The access token expires naturally (15 min).
     */
    void logout(LogoutRequest request);

    /**
     * Initiates a password reset by sending a reset link to the email.
     * Always succeeds — never reveals whether the email is registered.
     */
    void requestPasswordReset(PasswordResetRequest request);

    /**
     * Confirms a password reset.
     * Validates the token, updates the password, revokes all refresh tokens.
     * Throws {@link com.attendai.core.common.exception.ValidationException} on invalid/expired token.
     */
    void confirmPasswordReset(PasswordResetConfirmRequest request);
}
