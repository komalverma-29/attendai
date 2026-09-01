package com.attendai.school.school.dto;

import com.attendai.school.school.entity.SchoolStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeSchoolStatusRequest {

    @NotNull(message = "Status is required")
    private SchoolStatus status;

    @Size(max = 500)
    private String reason;
}
