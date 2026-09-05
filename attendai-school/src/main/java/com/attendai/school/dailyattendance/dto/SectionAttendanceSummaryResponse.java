package com.attendai.school.dailyattendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SectionAttendanceSummaryResponse {
    private final Long                           sectionId;
    private final LocalDate                      date;
    /** True when the queried date is a working day per the academic calendar. */
    private final boolean                        workingDay;
    private final List<StudentAttendanceDayResponse> records;
    private final int                            present;
    private final int                            late;
    private final int                            absent;
    private final int                            onLeave;
}
