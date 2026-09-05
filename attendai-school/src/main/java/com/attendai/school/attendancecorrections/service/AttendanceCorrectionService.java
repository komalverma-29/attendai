package com.attendai.school.attendancecorrections.service;

import com.attendai.school.attendancecorrections.dto.CorrectionRequestResponse;
import com.attendai.school.attendancecorrections.dto.CorrectionSummaryResponse;
import com.attendai.school.attendancecorrections.dto.CreateCorrectionRequest;
import com.attendai.school.attendancecorrections.dto.ReviewCorrectionRequest;
import com.attendai.school.attendancecorrections.entity.CorrectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AttendanceCorrectionService {
    CorrectionRequestResponse submitCorrection(Long schoolId, CreateCorrectionRequest request,
                                                Long requestedById);
    CorrectionRequestResponse findById(Long schoolId, Long id);
    Page<CorrectionSummaryResponse> listCorrections(Long schoolId, Long studentId,
                                                      CorrectionStatus status,
                                                      LocalDate fromDate, LocalDate toDate,
                                                      Pageable pageable);
    CorrectionRequestResponse approveCorrection(Long schoolId, Long id,
                                                 ReviewCorrectionRequest request, Long reviewerId);
    CorrectionRequestResponse rejectCorrection(Long schoolId, Long id,
                                                ReviewCorrectionRequest request, Long reviewerId);
    CorrectionRequestResponse cancelCorrection(Long schoolId, Long id, Long requesterId);
}
