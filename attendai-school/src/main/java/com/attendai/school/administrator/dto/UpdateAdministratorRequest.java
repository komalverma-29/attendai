package com.attendai.school.administrator.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** All fields optional — only non-null values are applied. */
@Getter
@Setter
public class UpdateAdministratorRequest {

    @Size(max = 100)
    private String designation;

    @Size(max = 500)
    private String notes;
}
