package com.attendai.school.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class SchoolOverviewResponse {
    private final LocalDate            date;
    private final boolean              workingDay;
    private final Long                 academicYearId;
    private final int                  totalStudents;
    private final int                  present;
    private final int                  late;
    private final int                  absent;
    private final int                  onLeave;
    private final BigDecimal           attendancePercentage;
    private final PendingActionsResponse pendingActions;
}
