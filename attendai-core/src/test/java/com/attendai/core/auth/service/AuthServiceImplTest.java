package com.attendai.core.auth.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.auth.config.SecurityProperties;
import com.attendai.core.auth.dto.LoginRequest;
import com.attendai.core.auth.dto.LogoutRequest;
import com.attendai.core.auth.dto.RefreshRequest;
import com.attendai.core.auth.entity.RefreshToken;
import com.attendai.core.auth.repository.PasswordResetTokenRepository;
import com.attendai.core.auth.repository.RefreshTokenRepository;
import com.attendai.core.common.exception.UnauthorizedException;
import com.attendai.core.notification.service.NotificationService;
import com.attendai.core.permission.service.PermissionService;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserService           userService;
    @Mock RoleService           roleService;
    @Mock PermissionService     permissionService;
    @Mock JwtService            jwtService;
    @Mock RefreshTokenRepository       refreshTokenRepository;
    @Mock PasswordResetTokenRepository resetTokenRepository;
    @Mock NotificationService   notificationService;
    @Mock AuditService          auditService;

    private final PasswordEncoder    passwordEncoder = new BCryptPasswordEncoder(4);
    private final SecurityProperties securityProperties = new SecurityProperties();

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userService, roleService, permissionService,
                jwtService, passwordEncoder,
                refreshTokenRepository, resetTokenRepository,
                notificationService, auditService, securityProperties);
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @Test
    void login_shouldReturnTokens_whenValidCredentials() {
        String rawPassword = "Password1";
        String hash = passwordEncoder.encode(rawPassword);
        var user = new UserAuthProjection(1L, "a@b.com", hash, "ACTIVE", false);

        when(userService.findByEmailForAuth("a@b.com")).thenReturn(Optional.of(user));
        when(roleService.findRolesByUserId(1L)).thenReturn(List.of());
        when(permissionService.findPermissionCodesByUserId(1L)).thenReturn(List.of());
        when(jwtService.generateAccessToken(anyLong(), anyString(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.countActiveByUserId(anyLong(), any())).thenReturn(0L);

        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword(rawPassword);

        var result = authService.login(req, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getExpiresIn()).isEqualTo(900L);
        verify(userService).updateLastLoginAt(1L);
    }

    @Test
    void login_shouldThrow401_whenPasswordIncorrect() {
        String hash = passwordEncoder.encode("CorrectPassword1");
        var user = new UserAuthProjection(1L, "a@b.com", hash, "ACTIVE", false);

        when(userService.findByEmailForAuth("a@b.com")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("WrongPassword1");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verify(userService, never()).updateLastLoginAt(anyLong());
    }

    @Test
    void login_shouldThrow401_whenEmailNotFound() {
        when(userService.findByEmailForAuth("unknown@b.com")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@b.com");
        req.setPassword("AnyPass1");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_shouldThrow401_whenAccountInactive() {
        String hash = passwordEncoder.encode("Password1");
        var user = new UserAuthProjection(1L, "a@b.com", hash, "INACTIVE", false);

        when(userService.findByEmailForAuth("a@b.com")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setEmail("a@b.com");
        req.setPassword("Password1");

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    @Test
    void refresh_shouldRevokeOldTokenAndIssueNew_whenValid() {
        String rawToken = "raw-refresh-token";
        String hash = AuthServiceImpl.sha256Hex(rawToken);

        var stored = RefreshToken.builder()
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        var user = new UserAuthProjection(1L, "a@b.com", "hash", "ACTIVE", false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));
        when(userService.findByIdForAuth(1L)).thenReturn(Optional.of(user));
        when(roleService.findRolesByUserId(1L)).thenReturn(List.of());
        when(permissionService.findPermissionCodesByUserId(1L)).thenReturn(List.of());
        when(jwtService.generateAccessToken(anyLong(), anyString(), any(), any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(anyLong())).thenReturn("new-refresh");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.countActiveByUserId(anyLong(), any())).thenReturn(0L);

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(rawToken);
        var result = authService.refresh(req);

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void refresh_shouldThrow401_whenTokenNotFound() {
        String rawToken = "nonexistent-token";
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(rawToken);

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Test
    void logout_shouldRevokeToken_whenFound() {
        String rawToken = "my-refresh-token";
        String hash = AuthServiceImpl.sha256Hex(rawToken);

        var stored = RefreshToken.builder()
                .userId(1L)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken(rawToken);
        authService.logout(req);

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void logout_shouldSucceedSilently_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("unknown-token");

        // Must not throw
        authService.logout(req);
    }

    // -------------------------------------------------------------------------
    // SHA-256 helper
    // -------------------------------------------------------------------------

    @Test
    void sha256Hex_shouldProduceDeterministicHash() {
        String h1 = AuthServiceImpl.sha256Hex("test");
        String h2 = AuthServiceImpl.sha256Hex("test");
        assertThat(h1).isEqualTo(h2).hasSize(64);
    }

    @Test
    void sha256Hex_shouldProduceDifferentHashesForDifferentInputs() {
        assertThat(AuthServiceImpl.sha256Hex("a")).isNotEqualTo(AuthServiceImpl.sha256Hex("b"));
    }
}
