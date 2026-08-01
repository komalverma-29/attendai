package com.attendai.core.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Size(max = 100, message = "Role code must not exceed 100 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$",
             message = "Role code must be uppercase and contain only letters, digits, and underscores")
    private String code;

    @NotBlank(message = "Role name is required")
    @Size(max = 255, message = "Role name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
