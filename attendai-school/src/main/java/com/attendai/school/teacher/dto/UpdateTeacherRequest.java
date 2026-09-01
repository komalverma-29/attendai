package com.attendai.school.teacher.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeacherRequest {

    @Size(max = 50)
    private String employeeCode;

    @Size(max = 100)
    private String designation;

    @Size(max = 255)
    private String qualification;

    @Size(max = 100)
    private String department;

    @Size(max = 500)
    private String notes;
}
