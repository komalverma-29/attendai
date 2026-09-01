package com.attendai.school.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStudentRequest {
    @Size(max = 5)   private String bloodGroup;
    @Size(max = 200) private String guardianName;
    @Size(max = 30)  private String guardianPhone;
    @Email @Size(max = 255) private String guardianEmail;
    @Size(max = 500) private String notes;
}
