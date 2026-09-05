package com.attendai.school.leave.service;

import com.attendai.school.leave.dto.CreateLeaveApplicationRequest;
import com.attendai.school.leave.dto.LeaveApplicationResponse;
import com.attendai.school.leave.dto.LeaveApplicationSummaryResponse;
import com.attendai.school.leave.dto.ReviewLeaveRequest;
import com.attendai.school.leave.entity.LeaveStatus;
import com.attendai.school.leave.entity.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface LeaveApplicationService {

    LeaveApplicationResponse createLeave(Long schoolId, CreateLeaveApplicationRequest request);

    LeaveApplicationResponse findById(Long schoolId, Long id);

    Page<LeaveApplicationSummaryResponse> listLeaves(Long schoolId, Long studentId,
                                                      Long teacherId, LeaveStatus status,
                                                      LeaveType leaveType,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      Pageable pageable);

    LeaveApplicationResponse approveLeave(Long schoolId, Long id, ReviewLeaveRequest request,
                                           Long actorUserId);

    LeaveApplicationResponse rejectLeave(Long schoolId, Long id, ReviewLeaveRequest request,
                                          Long actorUserId);

    LeaveApplicationResponse cancelLeave(Long schoolId, Long id, Long actorUserId);

    LeaveApplicationResponse revokeLeave(Long schoolId, Long id, Long actorUserId);
}
