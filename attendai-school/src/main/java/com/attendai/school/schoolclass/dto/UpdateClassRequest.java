package com.attendai.school.schoolclass.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateClassRequest {

    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String displayName;

    @Min(value = 1, message = "gradeOrder must be at least 1")
    private Integer gradeOrder;
}
