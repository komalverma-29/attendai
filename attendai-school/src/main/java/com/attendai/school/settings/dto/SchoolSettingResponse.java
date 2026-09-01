package com.attendai.school.settings.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** Full response for a single school setting, including audit timestamps. */
@Getter
@Builder
public class SchoolSettingResponse {
    private final String        key;
    private final String        value;
    private final String        defaultValue;
    private final String        description;
    private final LocalDateTime updatedAt;
    private final LocalDateTime createdAt;
}
