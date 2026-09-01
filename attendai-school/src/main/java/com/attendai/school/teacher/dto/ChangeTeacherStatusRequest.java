package com.attendai.school.teacher.dto;

import com.attendai.school.teacher.entity.TeacherStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeTeacherStatusRequest {

    @NotNull(message = "Status is required")
    private TeacherStatus status;
}
