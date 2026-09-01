package com.attendai.school.schoolclass.dto;

import com.attendai.school.schoolclass.entity.ClassStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassSummaryResponse {
    private final Long        id;
    private final String      name;
    private final String      displayName;
    private final int         gradeOrder;
    private final ClassStatus status;
}
