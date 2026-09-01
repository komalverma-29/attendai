package com.attendai.school.teacher.dto;

import com.attendai.school.teacher.entity.TeacherStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeacherSummaryResponse {
    private final Long          id;
    private final Long          personId;
    private final Long          userId;
    private final String        employeeCode;
    private final String        designation;
    private final TeacherStatus status;
}
