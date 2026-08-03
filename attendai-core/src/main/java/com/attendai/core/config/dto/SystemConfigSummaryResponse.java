package com.attendai.core.config.dto;

import lombok.Builder;
import lombok.Getter;

/** Lightweight summary used in paginated list responses. */
@Getter
@Builder
public class SystemConfigSummaryResponse {
    private final String configKey;
    private final String configValue;
    private final String module;
    private final String description;
}
