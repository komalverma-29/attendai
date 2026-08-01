package com.attendai.core.auth.service;

import com.attendai.core.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT token generation, signing, validation, and claims extraction service.
 *
 * <p>All tokens are signed with HMAC-SHA256 (HS256) using the secret configured
 * in {@link JwtProperties}. The secret must be at least 256 bits (32 characters).
 *
 * <p>Access token claims:
 * <ul>
 *   <li>{@code sub} — user ID as string</li>
 *   <li>{@code email} — user email</li>
 *   <li>{@code roles} — list of role codes</li>
 *   <li>{@code permissions} — list of permission codes</li>
 *   <li>{@code iat}, {@code exp} — standard JWT timestamps</li>
 * </ul>
 *
 * <p>Refresh tokens are opaque JWTs carrying only {@code sub}, {@code iat},
 * and {@code exp}. Their only purpose is to prove freshness — the database
 * hash is the authority.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_EMAIL       = "email";
    private static final String CLAIM_ROLES       = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TOKEN_TYPE  = "type";
    private static final String TYPE_ACCESS       = "access";
    private static final String TYPE_REFRESH      = "refresh";

    /** Clock-skew tolerance in seconds (30 s). */
    private static final long CLOCK_SKEW_SECONDS = 30L;

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT signing secret is not configured. Set the JWT_SECRET environment variable.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT signing secret must be at least 256 bits (32 characters).");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a signed JWT access token.
     *
     * @param userId      the user's surrogate ID
     * @param email       the user's email address
     * @param roles       list of role codes to embed
     * @param permissions list of permission codes to embed
     * @return signed JWT string
     */
    public String generateAccessToken(Long userId, String email,
                                      List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenExpiry());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a signed JWT refresh token.
     * The refresh token carries only the subject (user ID) and timestamps.
     *
     * @param userId the user's surrogate ID
     * @return signed JWT string (raw — must be hashed before persisting)
     */
    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getRefreshTokenExpiry());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .id(UUID.randomUUID().toString()) // jti — prevents exact duplicates
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    /**
     * Validates a JWT string.
     * Returns {@link Optional#empty()} when the token is invalid or expired.
     * Logs a warning for each failure category.
     *
     * @param token the raw JWT string
     * @return the parsed {@link Claims}, or empty on any failure
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return Optional.empty();
        } catch (JwtException e) {
            log.warn("JWT token is invalid: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error validating JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns {@code true} if the token is structurally valid and not expired.
     *
     * @param token the raw JWT string
     * @return true if valid
     */
    public boolean isTokenValid(String token) {
        return validateToken(token).isPresent();
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the user ID (subject) from a validated token.
     *
     * @param claims the parsed token claims
     * @return user ID as Long
     */
    public Long extractUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Extracts the email claim from a validated token.
     *
     * @param claims the parsed token claims
     * @return email string
     */
    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    /**
     * Extracts the list of permission codes from a validated access token.
     *
     * @param claims the parsed token claims
     * @return list of permission code strings
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(Claims claims) {
        Object perms = claims.get(CLAIM_PERMISSIONS);
        if (perms instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /**
     * Returns the access token TTL in seconds (for the {@code expiresIn} response field).
     *
     * @return TTL in seconds
     */
    public long getAccessTokenExpirySeconds() {
        return jwtProperties.getAccessTokenExpiry();
    }
}
