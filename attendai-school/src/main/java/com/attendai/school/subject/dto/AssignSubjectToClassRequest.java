package com.attendai.school.subject.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignSubjectToClassRequest {

    @NotNull(message = "Class id is required")
    private Long classId;
}
