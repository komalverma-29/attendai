package com.attendai.core.role.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleSummaryResponse {
    private final Long id;
    private final String code;
    private final String name;
    private final boolean isSystem;
}
