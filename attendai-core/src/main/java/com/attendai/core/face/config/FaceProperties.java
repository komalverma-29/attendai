package com.attendai.core.face.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Face recognition configuration properties.
 * Bound from {@code application.yml} under the prefix {@code attendai.face}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.face")
public class FaceProperties {

    /**
     * Minimum confidence score (0.0–1.0) required for a positive match.
     * Scores at or above this value are treated as matches.
     * Default: 0.85
     */
    private float recognitionThreshold = 0.85f;

    /**
     * Maximum number of face images allowed per profile.
     * Default: 10
     */
    private int maxImagesPerProfile = 10;

    /**
     * When true, the recognition endpoint runs liveness detection before matching.
     * Default: false (opt-in feature)
     */
    private boolean livenessCheckEnabled = false;
}
