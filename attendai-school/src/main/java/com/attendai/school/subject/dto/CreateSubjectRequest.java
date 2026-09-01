package com.attendai.school.subject.dto;

import com.attendai.school.subject.entity.SubjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSubjectRequest {

    @NotBlank(message = "Subject name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Subject code is required")
    @Size(max = 20, message = "Code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase letters, digits, or underscores")
    private String code;

    @NotNull(message = "Subject type is required")
    private SubjectType type;

    @Size(max = 500)
    private String description;
}
