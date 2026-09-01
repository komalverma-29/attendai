package com.attendai.school.academicyear.service;

import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.dto.AcademicYearSummaryResponse;
import com.attendai.school.academicyear.dto.CreateAcademicYearRequest;
import com.attendai.school.academicyear.dto.UpdateAcademicYearRequest;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface AcademicYearService {

    AcademicYearResponse createAcademicYear(Long schoolId, CreateAcademicYearRequest request);

    AcademicYearResponse findById(Long schoolId, Long id);

    Optional<AcademicYearResponse> getActiveAcademicYear(Long schoolId);

    AcademicYearResponse getActiveAcademicYearOrThrow(Long schoolId);

    Page<AcademicYearSummaryResponse> listAcademicYears(Long schoolId, AcademicYearStatus status,
                                                         Pageable pageable);

    AcademicYearResponse updateAcademicYear(Long schoolId, Long id, UpdateAcademicYearRequest request);

    AcademicYearResponse activateAcademicYear(Long schoolId, Long id);

    AcademicYearResponse completeAcademicYear(Long schoolId, Long id);

    AcademicYearResponse cancelAcademicYear(Long schoolId, Long id);

    void deleteAcademicYear(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by future Round-2+ modules
    // -------------------------------------------------------------------------

    boolean existsById(Long id);

    boolean isActive(Long id);

    boolean isDateWithinAcademicYear(Long academicYearId, LocalDate date);
}
