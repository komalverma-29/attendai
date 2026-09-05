package com.attendai.school.leave.dto;

import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LeaveApplicationSummaryResponse {
    private final Long               id;
    private final LeaveApplicantType applicantType;
    private final Long               studentId;
    private final Long               teacherId;
    private final LeaveType          leaveType;
    private final LocalDate          startDate;
    private final LocalDate          endDate;
    private final int                totalDays;
    private final LeaveStatus        status;
}
