package com.attendai.school.subject.dto;

import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SubjectResponse {
    private final Long          id;
    private final Long          schoolId;
    private final String        name;
    private final String        code;
    private final SubjectType   type;
    private final String        description;
    private final SubjectStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
