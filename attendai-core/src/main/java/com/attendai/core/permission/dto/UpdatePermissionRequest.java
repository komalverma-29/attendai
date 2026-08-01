package com.attendai.core.permission.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePermissionRequest {
    @Size(min = 1, max = 255)
    private String name;
    @Size(max = 1000)
    private String description;
}
