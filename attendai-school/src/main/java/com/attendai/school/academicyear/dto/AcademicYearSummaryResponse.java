package com.attendai.school.academicyear.dto;

import com.attendai.school.academicyear.entity.AcademicYearStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class AcademicYearSummaryResponse {
    private final Long               id;
    private final Long               schoolId;
    private final String             name;
    private final LocalDate          startDate;
    private final LocalDate          endDate;
    private final AcademicYearStatus status;
}
