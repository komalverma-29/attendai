package com.attendai.core.audit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Audit module configuration properties.
 *
 * Bound from {@code application.yml} under the prefix {@code attendai.audit}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.audit")
public class AuditProperties {

    /**
     * When {@code true}, the {@code X-Forwarded-For} header is trusted for
     * resolving the client IP address. Enable only when the application is
     * behind a trusted reverse proxy.
     * Default: {@code false}
     */
    private boolean trustProxy = false;

    /**
     * Maximum allowed length (in characters) of the {@code details} JSON string.
     * Values exceeding this limit are truncated before persistence.
     * Default: {@code 10000}
     */
    private int maxDetailsLength = 10_000;
}
