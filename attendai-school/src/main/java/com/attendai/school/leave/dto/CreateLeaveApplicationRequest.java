package com.attendai.school.leave.dto;

import com.attendai.school.leave.entity.LeaveApplicantType;
import com.attendai.school.leave.entity.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateLeaveApplicationRequest {

    @NotNull(message = "Applicant type is required")
    private LeaveApplicantType applicantType;

    /** Required when applicantType = STUDENT. */
    private Long studentId;

    /** Required when applicantType = TEACHER. */
    private Long teacherId;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    private Long evidenceFileId;

    private Long academicYearId;
}
