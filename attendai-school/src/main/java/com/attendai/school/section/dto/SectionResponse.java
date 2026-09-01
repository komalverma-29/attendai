package com.attendai.school.section.dto;

import com.attendai.school.section.entity.SectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SectionResponse {
    private final Long          id;
    private final Long          schoolId;
    private final Long          classId;
    private final Long          academicYearId;
    private final String        name;
    private final String        description;
    private final SectionStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
