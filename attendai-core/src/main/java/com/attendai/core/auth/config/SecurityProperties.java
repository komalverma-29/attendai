package com.attendai.core.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Security configuration properties.
 *
 * Bound from {@code application.yml} under the prefix {@code attendai.security}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.security")
public class SecurityProperties {

    /** BCrypt work factor. Minimum enforced value: 10. Default: 12. */
    private int bcryptStrength = 12;

    /** Maximum concurrent active refresh tokens per user. Default: 5. */
    private int maxSessions = 5;

    /** Password reset token TTL in seconds. Default: 3600 (1 hour). */
    private long resetTokenExpiry = 3_600L;

    /**
     * CORS allowed origins. Must be explicitly configured — wildcard (*) is
     * not permitted in production.
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
