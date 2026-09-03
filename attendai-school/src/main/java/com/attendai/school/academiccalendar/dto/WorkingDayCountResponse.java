package com.attendai.school.academiccalendar.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkingDayCountResponse {
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final int       workingDayCount;
}
