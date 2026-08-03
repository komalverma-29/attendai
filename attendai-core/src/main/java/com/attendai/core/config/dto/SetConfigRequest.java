package com.attendai.core.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetConfigRequest {

    @NotBlank(message = "Value is required")
    @Size(max = 1000, message = "Value must not exceed 1000 characters")
    private String value;

    @NotBlank(message = "Module is required")
    @Size(max = 50, message = "Module must not exceed 50 characters")
    @Pattern(regexp = "^[a-z0-9][a-z0-9.\\-]*$",
             message = "Module must be lowercase and contain only letters, digits, dots, and hyphens")
    private String module;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
