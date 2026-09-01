package com.attendai.school.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSectionRequest {

    @NotBlank(message = "Section name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    private String name;

    @Size(max = 255)
    private String description;
}
