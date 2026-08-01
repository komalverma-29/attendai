package com.attendai.core.permission.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PermissionResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final String module;
    private final String description;
    private final boolean isSystem;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
