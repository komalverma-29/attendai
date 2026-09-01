package com.attendai.school.teacher.dto;

import com.attendai.school.teacher.entity.TeacherStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherResponse {
    private final Long          id;
    private final Long          schoolId;
    private final Long          personId;
    private final Long          userId;
    private final String        employeeCode;
    private final String        designation;
    private final String        qualification;
    private final String        department;
    private final TeacherStatus status;
    private final String        notes;
    private final LocalDate     joiningDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
