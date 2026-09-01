package com.attendai.school.subject.dto;

import com.attendai.school.subject.entity.SubjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeSubjectStatusRequest {

    @NotNull(message = "Status is required")
    private SubjectStatus status;
}
