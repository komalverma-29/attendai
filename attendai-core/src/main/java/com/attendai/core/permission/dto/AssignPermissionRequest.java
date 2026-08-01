package com.attendai.core.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignPermissionRequest {
    @NotNull(message = "Permission ID is required")
    private Long permissionId;
}
