package com.attendai.school.subject.dto;

import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubjectSummaryResponse {
    private final Long          id;
    private final String        name;
    private final String        code;
    private final SubjectType   type;
    private final SubjectStatus status;
}
