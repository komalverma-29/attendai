package com.attendai.school.attendancecorrections.dto;

import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Builder
public class CorrectionRequestResponse {
    private final Long                  id;
    private final Long                  schoolId;
    private final Long                  academicYearId;
    private final Long                  studentId;
    private final Long                  attendanceRecordId;
    private final LocalDate             attendanceDate;
    private final DailyAttendanceStatus originalStatus;
    private final DailyAttendanceStatus requestedStatus;
    private final String                reason;
    private final CorrectionStatus      status;
    private final Long                  requestedById;
    private final Long                  reviewedById;
    private final LocalDateTime         reviewedAt;
    private final String                rejectionReason;
    private final LocalDateTime         createdAt;
    private final LocalDateTime         updatedAt;
}
