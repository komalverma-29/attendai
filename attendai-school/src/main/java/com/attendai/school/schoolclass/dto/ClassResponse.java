package com.attendai.school.schoolclass.dto;

import com.attendai.school.schoolclass.entity.ClassStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClassResponse {
    private final Long        id;
    private final Long        schoolId;
    private final String      name;
    private final String      displayName;
    private final int         gradeOrder;
    private final ClassStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
