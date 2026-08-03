package com.attendai.core.config.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Full system configuration response including audit timestamps. */
@Getter
@Builder
public class SystemConfigResponse {
    private final Long          id;
    private final String        configKey;
    private final String        configValue;
    private final String        module;
    private final String        description;
    private final LocalDateTime updatedAt;
    private final LocalDateTime createdAt;
}
