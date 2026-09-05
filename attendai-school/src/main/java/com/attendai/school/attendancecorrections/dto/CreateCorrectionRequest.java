package com.attendai.school.attendancecorrections.dto;

import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class CreateCorrectionRequest {
    @NotNull(message = "Student id is required")           private Long                  studentId;
    @NotNull(message = "Attendance date is required")      private LocalDate             attendanceDate;
    @NotNull(message = "Requested status is required")     private DailyAttendanceStatus requestedStatus;
    @NotBlank(message = "Reason is required")
    @Size(max = 1000)                                      private String                reason;
    private Long evidenceFileId;
}
