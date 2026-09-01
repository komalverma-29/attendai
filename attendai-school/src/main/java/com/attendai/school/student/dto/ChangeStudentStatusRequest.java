package com.attendai.school.student.dto;

import com.attendai.school.student.entity.StudentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStudentStatusRequest {

    @NotNull(message = "Status is required")
    private StudentStatus status;

    private String reason;
}
