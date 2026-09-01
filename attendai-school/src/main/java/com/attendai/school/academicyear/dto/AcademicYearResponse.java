package com.attendai.school.academicyear.dto;

import com.attendai.school.academicyear.entity.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AcademicYearResponse {
    private final Long               id;
    private final Long               schoolId;
    private final String             name;
    private final LocalDate          startDate;
    private final LocalDate          endDate;
    private final AcademicYearStatus status;
    private final String             description;
    private final LocalDateTime      createdAt;
    private final LocalDateTime      updatedAt;
}
