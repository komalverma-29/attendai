package com.attendai.school.section.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EnrollStudentInSectionRequest {

    @NotNull(message = "Student id is required")
    private Long studentId;

    @NotBlank(message = "Roll number is required")
    @Size(max = 20, message = "Roll number must not exceed 20 characters")
    private String rollNumber;

    @NotNull(message = "Enrollment date is required")
    private LocalDate enrolledAt;
}
