package com.attendai.core.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Attendance engine configuration properties.
 * Bound from {@code application.yml} under the prefix {@code attendai.attendance}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "attendai.attendance")
public class AttendanceProperties {

    /**
     * Default deduplication window in seconds.
     * Within this window, identical (person, station, direction) events are
     * stored as DUPLICATE rather than PENDING.
     * Default: 300 (5 minutes).
     */
    private long dedupWindowSeconds = 300L;

    /**
     * Maximum seconds an event time can be in the future (clock skew tolerance).
     * Default: 60 (1 minute).
     */
    private long maxFutureSeconds = 60L;

    /**
     * Maximum hours an event time can be backdated.
     * Default: 24.
     */
    private long maxPastHours = 24L;
}
