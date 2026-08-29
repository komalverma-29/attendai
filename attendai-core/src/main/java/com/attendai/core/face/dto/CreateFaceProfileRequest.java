package com.attendai.core.face.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFaceProfileRequest {

    @NotNull(message = "personId is required")
    private Long personId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
