package com.attendai.school.teacherassignment.dto;

import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeAssignmentStatusRequest {

    @NotNull(message = "Status is required")
    private AssignmentStatus status;
}
