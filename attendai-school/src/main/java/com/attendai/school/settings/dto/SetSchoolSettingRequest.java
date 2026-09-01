package com.attendai.school.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request body for creating or updating a school setting. */
@Getter
@Setter
public class SetSchoolSettingRequest {

    @NotBlank(message = "Value is required")
    @Size(max = 1000, message = "Value must not exceed 1000 characters")
    private String value;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
