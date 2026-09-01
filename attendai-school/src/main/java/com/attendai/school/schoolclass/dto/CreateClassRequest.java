package com.attendai.school.schoolclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 100)
    private String displayName;

    @NotNull(message = "gradeOrder is required")
    @Min(value = 1, message = "gradeOrder must be at least 1")
    private Integer gradeOrder;
}
