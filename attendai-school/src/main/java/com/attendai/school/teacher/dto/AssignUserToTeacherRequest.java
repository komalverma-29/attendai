package com.attendai.school.teacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignUserToTeacherRequest {

    @NotNull(message = "userId is required")
    private Long userId;
}
