package com.attendai.school.teacher.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTeacherRequest {

    @NotNull(message = "personId is required")
    private Long personId;

    /** Optional — teacher may not have platform login. */
    private Long userId;

    @Size(max = 50, message = "Employee code must not exceed 50 characters")
    private String employeeCode;

    @Size(max = 100)
    private String designation;

    @Size(max = 255)
    private String qualification;

    @Size(max = 100)
    private String department;

    @Past(message = "Joining date must be in the past or today")
    private LocalDate joiningDate;

    @Size(max = 500)
    private String notes;
}
