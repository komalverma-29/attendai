package com.attendai.school.leave.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewLeaveRequest {

    @Size(max = 1000)
    private String remarks;

    @Size(max = 1000)
    private String rejectionReason;
}
