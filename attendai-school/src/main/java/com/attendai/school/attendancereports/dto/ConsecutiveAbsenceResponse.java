package com.attendai.school.attendancereports.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter @Builder
public class ConsecutiveAbsenceResponse {
    private final Long      studentId;
    private final Long      sectionId;
    private final LocalDate streakStartDate;
    private final int       consecutiveDays;
}
