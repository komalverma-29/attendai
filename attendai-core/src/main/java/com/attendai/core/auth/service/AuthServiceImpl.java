package com.attendai.core.auth.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.auth.config.SecurityProperties;
import com.attendai.core.auth.dto.AuthTokenResponse;
import com.attendai.core.auth.dto.LoginRequest;
import com.attendai.core.auth.dto.LogoutRequest;
import com.attendai.core.auth.dto.PasswordResetConfirmRequest;
import com.attendai.core.auth.dto.PasswordResetRequest;
import com.attendai.core.auth.dto.RefreshRequest;
import com.attendai.core.auth.entity.PasswordResetToken;
import com.attendai.core.auth.entity.RefreshToken;
import com.attendai.core.auth.repository.PasswordResetTokenRepository;
import com.attendai.core.auth.repository.RefreshTokenRepository;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.notification.dto.SendNotificationRequest;
import com.attendai.core.notification.service.NotificationService;
import com.attendai.core.permission.service.PermissionService;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Core authentication service implementation.
 *
 * Orchestrates all login, refresh, logout, and password-reset flows.
 * Delegates token generation to {@link JwtService}, password hashing to
 * {@link PasswordEncoder}, and notification to {@link NotificationService}.
 *
 * Security rules enforced here:
 * - Login failures return a generic "Invalid credentials" message — never reveal
 *   whether the email exists.
 * - Password reset requests always return 200 — never reveal email existence.
 * - Token reuse detection revokes ALL tokens for the affected user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService           userService;
    private final RoleService           roleService;
    private final PermissionService     permissionService;
    private final JwtService            jwtService;
    private final PasswordEncoder       passwordEncoder;
    private final RefreshTokenRepository       refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final NotificationService   notificationService;
    private final AuditService          auditService;
    private final SecurityProperties    securityProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthTokenResponse login(LoginRequest request, String ipAddress) {
        var userOpt = userService.findByEmailForAuth(request.getEmail());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().passwordHash())) {
            log.warn("Login failed for email: [REDACTED] | ip={}", ipAddress);
            auditService.log(AuditEventRequest.builder()
                    .actionCode("AUTH_LOGIN_FAILURE")
                    .module(AttendAIConstants.MODULE_CORE)
                    .ipAddress(ipAddress)
                    .details("{\"reason\":\"bad_credentials\"}")
                    .build());
            throw new UnauthorizedException("Invalid credentials");
        }

        var user = userOpt.get();

        if (!user.isActive()) {
            log.warn("Login rejected — account not active | userId={}", user.id());
            auditService.log(AuditEventRequest.builder()
                    .actionCode("AUTH_LOGIN_FAILURE")
                    .module(AttendAIConstants.MODULE_CORE)
                    .actorUserId(user.id())
                    .ipAddress(ipAddress)
                    .details("{\"reason\":\"account_not_active\"}")
                    .build());
            throw new UnauthorizedException("Invalid credentials");
        }

        // Load roles + permissions
        List<String> roleCodes = roleService.findRolesByUserId(user.id())
                .stream().map(r -> r.code()).toList();
        List<String> permCodes = permissionService.findPermissionCodesByUserId(user.id());

        // Issue tokens
        String accessToken  = jwtService.generateAccessToken(user.id(), user.email(), roleCodes, permCodes);
        String rawRefresh   = jwtService.generateRefreshToken(user.id());
        storeRefreshToken(user.id(), rawRefresh);

        userService.updateLastLoginAt(user.id());

        log.info("Login success | userId={}", user.id());
        auditService.log(AuditEventRequest.builder()
                .actionCode("AUTH_LOGIN_SUCCESS")
                .module(AttendAIConstants.MODULE_CORE)
                .actorUserId(user.id())
                .resourceType("User")
                .resourceId(String.valueOf(user.id()))
                .ipAddress(ipAddress)
                .build());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefresh)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .build();
    }

    // -------------------------------------------------------------------------
    // Token refresh
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AuthTokenResponse refresh(RefreshRequest request) {
        String hash = sha256Hex(request.getRefreshToken());
        var tokenOpt = refreshTokenRepository.findByTokenHash(hash);

        if (tokenOpt.isEmpty()) {
            log.warn("Refresh attempt with unknown token hash");
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken stored = tokenOpt.get();

        // Token reuse detection — submitted token is already revoked
        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected for userId={} — revoking all tokens", stored.getUserId());
            refreshTokenRepository.revokeAllByUserId(stored.getUserId(), LocalDateTime.now());
            auditService.log(AuditEventRequest.builder()
                    .actionCode("AUTH_TOKEN_REUSE")
                    .module(AttendAIConstants.MODULE_CORE)
                    .actorUserId(stored.getUserId())
                    .details("{\"action\":\"all_tokens_revoked\"}")
                    .build());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Revoke old token (rotation)
        stored.revoke();
        refreshTokenRepository.save(stored);

        Long userId = stored.getUserId();
        var userOpt = userService.findByIdForAuth(userId);
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            throw new UnauthorizedException("User account is not active");
        }
        var user = userOpt.get();

        List<String> roleCodes = roleService.findRolesByUserId(userId).stream().map(r -> r.code()).toList();
        List<String> permCodes = permissionService.findPermissionCodesByUserId(userId);

        String newAccessToken  = jwtService.generateAccessToken(userId, user.email(), roleCodes, permCodes);
        String newRawRefresh   = jwtService.generateRefreshToken(userId);
        storeRefreshToken(userId, newRawRefresh);

        log.info("Token refreshed | userId={}", userId);
        auditService.log(AuditEventRequest.builder()
                .actionCode("AUTH_TOKEN_REFRESH")
                .module(AttendAIConstants.MODULE_CORE)
                .actorUserId(userId)
                .build());

        return AuthTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefresh)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .build();
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String hash = sha256Hex(request.getRefreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
            log.info("Logout | userId={}", token.getUserId());
            auditService.log(AuditEventRequest.builder()
                    .actionCode("AUTH_LOGOUT")
                    .module(AttendAIConstants.MODULE_CORE)
                    .actorUserId(token.getUserId())
                    .build());
        });
        // If token is not found, silently succeed (idempotent logout)
    }

    // -------------------------------------------------------------------------
    // Password reset — request
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        var userOpt = userService.findByEmailForAuth(request.getEmail());
        if (userOpt.isEmpty()) {
            // Silently succeed — do not reveal email existence
            log.debug("Password reset requested for unknown email");
            return;
        }

        var user = userOpt.get();

        // Invalidate any existing unused reset tokens
        resetTokenRepository.invalidateAllByUserId(user.id(), LocalDateTime.now());

        // Generate and store new reset token
        String rawToken = generateSecureToken();
        String hash = sha256Hex(rawToken);
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(securityProperties.getResetTokenExpiry());

        resetTokenRepository.save(PasswordResetToken.builder()
                .userId(user.id())
                .tokenHash(hash)
                .expiresAt(expiry)
                .build());

        // Send notification (fire-and-forget — never expose failure)
        try {
            notificationService.send(SendNotificationRequest.builder()
                    .recipientUserId(user.id())
                    .typeCode("AUTH_PASSWORD_RESET")
                    .channels(List.of("EMAIL"))
                    .variables(Map.of("resetToken", rawToken))
                    .build());
        } catch (Exception e) {
            log.error("Failed to send password reset notification for userId={}: {}", user.id(), e.getMessage());
        }

        log.info("Password reset token issued | userId={}", user.id());
        auditService.log(AuditEventRequest.builder()
                .actionCode("AUTH_RESET_REQUEST")
                .module(AttendAIConstants.MODULE_CORE)
                .actorUserId(user.id())
                .build());
    }

    // -------------------------------------------------------------------------
    // Password reset — confirm
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        String hash = sha256Hex(request.getToken());
        var tokenOpt = resetTokenRepository.findByTokenHash(hash);

        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            String reason = tokenOpt.isEmpty() ? "not_found"
                    : tokenOpt.get().isUsed() ? "already_used" : "expired";
            throw new ValidationException("Password reset token is invalid or has expired");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Hash and update the new password
        String newHash = passwordEncoder.encode(request.getNewPassword());
        userService.updatePasswordHash(resetToken.getUserId(), newHash);

        // Mark token as used
        resetToken.markUsed();
        resetTokenRepository.save(resetToken);

        // Revoke all refresh tokens (force re-login everywhere)
        refreshTokenRepository.revokeAllByUserId(resetToken.getUserId(), LocalDateTime.now());

        log.info("Password reset confirmed | userId={}", resetToken.getUserId());
        auditService.log(AuditEventRequest.builder()
                .actionCode("AUTH_RESET_SUCCESS")
                .module(AttendAIConstants.MODULE_CORE)
                .actorUserId(resetToken.getUserId())
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void storeRefreshToken(Long userId, String rawToken) {
        enforceSessionLimit(userId);

        String hash = sha256Hex(rawToken);
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(
                jwtService.getAccessTokenExpirySeconds() *
                        (securityProperties.getMaxSessions() > 0 ? 1 : 1));  // expiry from JWT claims
        // Derive actual refresh expiry from the token itself (simpler: use config property)
        long refreshTtl = 604_800L; // 7 days default — JwtService manages this
        expiry = LocalDateTime.now().plusSeconds(refreshTtl);

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiry)
                .build());
    }

    private void enforceSessionLimit(Long userId) {
        long active = refreshTokenRepository.countActiveByUserId(userId, LocalDateTime.now());
        if (active >= securityProperties.getMaxSessions()) {
            // Revoke the oldest active token to stay within the session limit
            refreshTokenRepository.findOldestActiveByUserId(userId, LocalDateTime.now())
                    .ifPresent(oldest -> {
                        oldest.revoke();
                        refreshTokenRepository.save(oldest);
                        log.info("Session limit reached — oldest token revoked | userId={}", userId);
                    });
        }
    }

    /**
     * Computes the SHA-256 hex digest of the given token string.
     * Used for all token hash lookups and storage.
     */
    static String sha256Hex(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /** Generates a cryptographically random 32-byte token encoded as Base64 URL-safe. */
    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
