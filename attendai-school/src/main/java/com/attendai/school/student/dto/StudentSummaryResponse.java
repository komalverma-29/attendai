package com.attendai.school.student.dto;

import com.attendai.school.student.entity.StudentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentSummaryResponse {
    private final Long          id;
    private final Long          personId;
    private final Long          userId;
    private final String        admissionNumber;
    private final StudentStatus status;
}
