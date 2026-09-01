package com.attendai.school.administrator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdministratorRequest {

    @NotNull(message = "personId is required")
    private Long personId;

    @NotNull(message = "userId is required")
    private Long userId;

    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
