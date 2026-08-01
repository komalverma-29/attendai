package com.attendai.core.auth.service;

import com.attendai.core.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties props;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("super-secret-key-that-is-at-least-32-characters-long!");
        props.setAccessTokenExpiry(900L);
        props.setRefreshTokenExpiry(604800L);
        props.setAlgorithm("HS256");

        jwtService = new JwtService(props);
        jwtService.init();
    }

    // -------------------------------------------------------------------------
    // Access token generation and validation
    // -------------------------------------------------------------------------

    @Test
    void generateAccessToken_shouldProduceValidToken() {
        String token = jwtService.generateAccessToken(1L, "test@example.com",
                List.of("SCHOOL_ADMIN"), List.of("SCHOOL_STUDENT_READ"));

        assertThat(token).isNotBlank();
        Optional<Claims> claims = jwtService.validateToken(token);
        assertThat(claims).isPresent();
    }

    @Test
    void validateToken_shouldExtractCorrectClaims() {
        String token = jwtService.generateAccessToken(42L, "user@example.com",
                List.of("ROLE_A"), List.of("PERM_X", "PERM_Y"));

        Claims claims = jwtService.validateToken(token).orElseThrow();

        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(claims)).isEqualTo("user@example.com");
        assertThat(jwtService.extractPermissions(claims)).containsExactlyInAnyOrder("PERM_X", "PERM_Y");
    }

    @Test
    void validateToken_shouldReturnEmpty_forTamperedToken() {
        String token = jwtService.generateAccessToken(1L, "a@b.com", List.of(), List.of());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.validateToken(tampered)).isEmpty();
    }

    @Test
    void validateToken_shouldReturnEmpty_forExpiredToken() {
        // Use -120s so the token is well past the 30-second clock-skew leeway
        props.setAccessTokenExpiry(-120L);
        jwtService = new JwtService(props);
        jwtService.init();

        String token = jwtService.generateAccessToken(1L, "a@b.com", List.of(), List.of());

        assertThat(jwtService.validateToken(token)).isEmpty();
    }

    @Test
    void validateToken_shouldReturnEmpty_forGarbage() {
        assertThat(jwtService.validateToken("not.a.jwt")).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Refresh token
    // -------------------------------------------------------------------------

    @Test
    void generateRefreshToken_shouldProduceValidToken() {
        String token = jwtService.generateRefreshToken(5L);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isPresent();
    }

    @Test
    void refreshToken_shouldCarryUserId() {
        String token = jwtService.generateRefreshToken(99L);
        Claims claims = jwtService.validateToken(token).orElseThrow();

        assertThat(jwtService.extractUserId(claims)).isEqualTo(99L);
    }

    @Test
    void refreshToken_permissionsShouldBeEmpty() {
        String token = jwtService.generateRefreshToken(1L);
        Claims claims = jwtService.validateToken(token).orElseThrow();

        assertThat(jwtService.extractPermissions(claims)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Init validation
    // -------------------------------------------------------------------------

    @Test
    void init_shouldThrow_whenSecretIsNull() {
        JwtProperties badProps = new JwtProperties();
        JwtService bad = new JwtService(badProps);

        assertThatThrownBy(bad::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT signing secret is not configured");
    }

    @Test
    void init_shouldThrow_whenSecretIsTooShort() {
        JwtProperties badProps = new JwtProperties();
        badProps.setSecret("short");
        JwtService bad = new JwtService(badProps);

        assertThatThrownBy(bad::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
