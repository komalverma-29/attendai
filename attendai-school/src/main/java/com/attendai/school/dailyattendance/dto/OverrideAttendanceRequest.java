package com.attendai.school.dailyattendance.dto;

import com.attendai.school.dailyattendance.entity.DailyAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OverrideAttendanceRequest {

    @NotNull(message = "Status is required")
    private DailyAttendanceStatus status;

    @Size(max = 500)
    private String remarks;
}
