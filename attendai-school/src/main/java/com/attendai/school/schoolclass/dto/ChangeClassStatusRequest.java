package com.attendai.school.schoolclass.dto;

import com.attendai.school.schoolclass.entity.ClassStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeClassStatusRequest {

    @NotNull(message = "Status is required")
    private ClassStatus status;
}
