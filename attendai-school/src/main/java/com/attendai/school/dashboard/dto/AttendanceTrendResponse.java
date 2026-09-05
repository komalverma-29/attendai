package com.attendai.school.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AttendanceTrendResponse {
    private final Long             sectionId;
    private final List<TrendPoint> trend;

    @Getter
    @Builder
    public static class TrendPoint {
        private final LocalDate  date;
        private final BigDecimal percentage;
    }
}
