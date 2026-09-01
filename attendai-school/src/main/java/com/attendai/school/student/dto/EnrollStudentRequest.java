package com.attendai.school.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EnrollStudentRequest {

    @NotNull(message = "personId is required")
    private Long personId;

    @NotBlank(message = "admissionNumber is required")
    @Size(max = 50)
    private String admissionNumber;

    @NotNull(message = "enrollmentDate is required")
    @PastOrPresent(message = "Enrollment date must not be in the future")
    private LocalDate enrollmentDate;

    @Size(max = 5)
    private String bloodGroup;

    @Size(max = 200)
    private String guardianName;

    @Size(max = 30)
    private String guardianPhone;

    @Email
    @Size(max = 255)
    private String guardianEmail;

    @Size(max = 500)
    private String notes;
}
