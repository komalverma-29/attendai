package com.attendai.core.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration properties.
 *
 * Bound from {@code application.yml} under the prefix {@code attendai.jwt}.
 *
 * The signing secret is injected via the environment variable {@code JWT_SECRET}
 * and referenced as {@code ${JWT_SECRET}} in {@code application.yml}.
 * It must never be hardcoded.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.jwt")
public class JwtProperties {

    /**
     * HS256 signing secret. Must be at least 256 bits (32 characters).
     * Injected via environment variable JWT_SECRET.
     */
    private String secret;

    /** Access token TTL in seconds. Default: 900 (15 minutes). */
    private long accessTokenExpiry = 900L;

    /** Refresh token TTL in seconds. Default: 604800 (7 days). */
    private long refreshTokenExpiry = 604_800L;

    /** Signing algorithm. Supported values: HS256, RS256. Default: HS256. */
    private String algorithm = "HS256";
}
