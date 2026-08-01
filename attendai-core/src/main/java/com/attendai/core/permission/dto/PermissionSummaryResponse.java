package com.attendai.core.permission.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionSummaryResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final String module;
    private final boolean isSystem;
}
