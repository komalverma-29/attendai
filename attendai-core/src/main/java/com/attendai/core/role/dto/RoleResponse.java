package com.attendai.core.role.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoleResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final String description;
    private final boolean isSystem;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
