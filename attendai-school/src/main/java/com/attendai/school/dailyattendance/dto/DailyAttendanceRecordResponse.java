package com.attendai.school.dailyattendance.dto;

import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class DailyAttendanceRecordResponse {
    private final Long                  id;
    private final Long                  schoolId;
    private final Long                  academicYearId;
    private final Long                  sectionId;
    private final Long                  studentId;
    private final LocalDate             attendanceDate;
    private final DailyAttendanceStatus status;
    private final LocalTime             arrivalTime;
    private final Long                  coreEventId;
    private final String                remarks;
    private final Long                  markedById;
    private final LocalDateTime         createdAt;
    private final LocalDateTime         updatedAt;
}
