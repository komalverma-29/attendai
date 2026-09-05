package com.attendai.school.leave.dto;

import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class LeaveApplicationResponse {
    private final Long               id;
    private final Long               schoolId;
    private final Long               academicYearId;
    private final LeaveApplicantType applicantType;
    private final Long               studentId;
    private final Long               teacherId;
    private final LeaveType          leaveType;
    private final LocalDate          startDate;
    private final LocalDate          endDate;
    private final int                totalDays;
    private final Integer            workingDays;
    private final String             reason;
    private final LeaveStatus        status;
    private final Long               approvedById;
    private final LocalDateTime      approvedAt;
    private final String             rejectionReason;
    private final Long               revokedById;
    private final LocalDateTime      revokedAt;
    private final LocalDateTime      createdAt;
    private final LocalDateTime      updatedAt;
}
