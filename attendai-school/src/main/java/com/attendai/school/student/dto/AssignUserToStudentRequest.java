package com.attendai.school.student.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignUserToStudentRequest {

    @NotNull(message = "userId is required")
    private Long userId;
}
