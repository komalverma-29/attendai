package com.attendai.school.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SectionDailySummaryResponse {
    private final Long       sectionId;
    private final String     sectionName;
    private final int        totalStudents;
    private final int        present;
    private final int        late;
    private final int        absent;
    private final int        onLeave;
    private final BigDecimal attendancePercentage;
}
