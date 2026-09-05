package com.attendai.school.attendancecorrections.dto;

import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter @Builder
public class CorrectionSummaryResponse {
    private final Long                  id;
    private final Long                  studentId;
    private final LocalDate             attendanceDate;
    private final DailyAttendanceStatus originalStatus;
    private final DailyAttendanceStatus requestedStatus;
    private final CorrectionStatus      status;
    private final Long                  requestedById;
}
