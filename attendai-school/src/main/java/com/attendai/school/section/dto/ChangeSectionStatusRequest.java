package com.attendai.school.section.dto;

import com.attendai.school.section.entity.SectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeSectionStatusRequest {

    @NotNull(message = "Status is required")
    private SectionStatus status;
}
