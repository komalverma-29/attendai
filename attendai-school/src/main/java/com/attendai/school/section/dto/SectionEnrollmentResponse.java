package com.attendai.school.section.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SectionEnrollmentResponse {
    private final Long      id;
    private final Long      sectionId;
    private final Long      studentId;
    private final Long      academicYearId;
    private final String    rollNumber;
    private final LocalDate enrolledAt;
    private final LocalDateTime createdAt;
}
