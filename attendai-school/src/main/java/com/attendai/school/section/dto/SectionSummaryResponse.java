package com.attendai.school.section.dto;

import com.attendai.school.section.entity.SectionStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SectionSummaryResponse {
    private final Long          id;
    private final String        name;
    private final String        description;
    private final SectionStatus status;
    private final long          studentCount;
}
