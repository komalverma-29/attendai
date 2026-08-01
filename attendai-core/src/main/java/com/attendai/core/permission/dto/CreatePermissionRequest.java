package com.attendai.core.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePermissionRequest {

    @NotBlank(message = "Permission code is required")
    @Size(max = 100)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Permission code must be uppercase with letters, digits, and underscores")
    private String code;

    @NotBlank(message = "Permission name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Module is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z]+$", message = "Module must be uppercase letters only")
    private String module;

    @Size(max = 1000)
    private String description;
}
