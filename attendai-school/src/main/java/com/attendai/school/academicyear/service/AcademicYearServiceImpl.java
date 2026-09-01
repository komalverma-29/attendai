package com.attendai.school.academicyear.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.dto.AcademicYearSummaryResponse;
import com.attendai.school.academicyear.dto.CreateAcademicYearRequest;
import com.attendai.school.academicyear.dto.UpdateAcademicYearRequest;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.entity.SchoolAcademicYear;
import com.attendai.school.academicyear.exception.AcademicYearNotFoundException;
import com.attendai.school.academicyear.exception.ActiveAcademicYearAlreadyExistsException;
import com.attendai.school.academicyear.mapper.AcademicYearMapper;
import com.attendai.school.academicyear.repository.SchoolAcademicYearRepository;
import com.attendai.school.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private static final String MODULE = "school";

    private final SchoolAcademicYearRepository yearRepository;
    private final AcademicYearMapper           yearMapper;
    private final SchoolService                schoolService;
    private final AuditService                 auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AcademicYearResponse createAcademicYear(Long schoolId,
                                                    CreateAcademicYearRequest request) {
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        validateDateOrder(request.getStartDate(), request.getEndDate());

        if (yearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Academic year name '" + request.getName()
                    + "' already exists for school " + schoolId);
        }
        validateNoOverlap(schoolId, Long.MIN_VALUE, request.getStartDate(), request.getEndDate());

        SchoolAcademicYear year = SchoolAcademicYear.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .status(AcademicYearStatus.UPCOMING)
                .build();

        SchoolAcademicYear saved = yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_CREATED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId + "}")
                .build());

        return yearMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse findById(Long schoolId, Long id) {
        return yearMapper.toResponse(requireYear(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicYearResponse> getActiveAcademicYear(Long schoolId) {
        return yearRepository.findBySchoolIdAndStatus(schoolId, AcademicYearStatus.ACTIVE)
                .map(yearMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getActiveAcademicYearOrThrow(Long schoolId) {
        return getActiveAcademicYear(schoolId)
                .orElseThrow(() -> new AcademicYearNotFoundException(
                        "No ACTIVE academic year found for school " + schoolId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AcademicYearSummaryResponse> listAcademicYears(Long schoolId,
                                                                AcademicYearStatus status,
                                                                Pageable pageable) {
        return yearRepository.findBySchoolIdAndOptionalStatus(schoolId, status, pageable)
                .map(yearMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AcademicYearResponse updateAcademicYear(Long schoolId, Long id,
                                                    UpdateAcademicYearRequest request) {
        SchoolAcademicYear year = requireYear(schoolId, id);

        // BR-AY-05: COMPLETED year is fully immutable
        if (AcademicYearStatus.COMPLETED.equals(year.getStatus())) {
            throw new ValidationException("A COMPLETED academic year is immutable");
        }

        // Name update is allowed for UPCOMING and ACTIVE
        if (request.getName() != null && !request.getName().equals(year.getName())) {
            if (yearRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
                throw new ResourceAlreadyExistsException(
                        "Academic year name '" + request.getName()
                        + "' already exists for school " + schoolId);
            }
            year.setName(request.getName());
        }

        // BR-AY-04: Date-range updates only for UPCOMING
        if (request.getStartDate() != null || request.getEndDate() != null) {
            if (!AcademicYearStatus.UPCOMING.equals(year.getStatus())) {
                throw new ValidationException(
                        "Date range can only be modified for UPCOMING academic years");
            }
            LocalDate newStart = request.getStartDate() != null
                    ? request.getStartDate() : year.getStartDate();
            LocalDate newEnd   = request.getEndDate() != null
                    ? request.getEndDate() : year.getEndDate();
            validateDateOrder(newStart, newEnd);
            validateNoOverlap(schoolId, id, newStart, newEnd);
            year.setStartDate(newStart);
            year.setEndDate(newEnd);
        }

        if (request.getDescription() != null) {
            year.setDescription(request.getDescription());
        }

        SchoolAcademicYear saved = yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_UPDATED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(id)).build());

        return yearMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public AcademicYearResponse activateAcademicYear(Long schoolId, Long id) {
        SchoolAcademicYear year = requireYear(schoolId, id);

        if (!AcademicYearStatus.UPCOMING.equals(year.getStatus())) {
            throw new ValidationException(
                    "Only an UPCOMING academic year can be activated; current status: "
                    + year.getStatus());
        }
        // BR-AY-01: enforce single ACTIVE per school
        yearRepository.findBySchoolIdAndStatus(schoolId, AcademicYearStatus.ACTIVE)
                .ifPresent(active -> {
                    throw new ActiveAcademicYearAlreadyExistsException(schoolId);
                });

        year.setStatus(AcademicYearStatus.ACTIVE);
        SchoolAcademicYear saved = yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_ACTIVATED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(id)).build());

        return yearMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AcademicYearResponse completeAcademicYear(Long schoolId, Long id) {
        SchoolAcademicYear year = requireYear(schoolId, id);

        if (!AcademicYearStatus.ACTIVE.equals(year.getStatus())) {
            throw new ValidationException(
                    "Only an ACTIVE academic year can be completed; current status: "
                    + year.getStatus());
        }

        year.setStatus(AcademicYearStatus.COMPLETED);
        SchoolAcademicYear saved = yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_COMPLETED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(id)).build());

        return yearMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AcademicYearResponse cancelAcademicYear(Long schoolId, Long id) {
        SchoolAcademicYear year = requireYear(schoolId, id);

        if (!AcademicYearStatus.UPCOMING.equals(year.getStatus())) {
            throw new ValidationException(
                    "Only an UPCOMING academic year can be cancelled; current status: "
                    + year.getStatus());
        }

        year.setStatus(AcademicYearStatus.CANCELLED);
        SchoolAcademicYear saved = yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_CANCELLED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(id)).build());

        return yearMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteAcademicYear(Long schoolId, Long id) {
        SchoolAcademicYear year = requireYear(schoolId, id);

        // BR-AY-07: only CANCELLED or UPCOMING (no dependencies — section check is future)
        if (AcademicYearStatus.ACTIVE.equals(year.getStatus())
                || AcademicYearStatus.COMPLETED.equals(year.getStatus())) {
            throw new ValidationException(
                    "Cannot delete an " + year.getStatus() + " academic year");
        }

        year.softDelete();
        yearRepository.save(year);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ACADEMIC_YEAR_DELETED")
                .module(MODULE).resourceType("SchoolAcademicYear")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return yearRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return yearRepository.findById(id)
                .map(y -> AcademicYearStatus.ACTIVE.equals(y.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDateWithinAcademicYear(Long academicYearId, LocalDate date) {
        return yearRepository.findById(academicYearId)
                .map(y -> !date.isBefore(y.getStartDate()) && !date.isAfter(y.getEndDate()))
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolAcademicYear requireYear(Long schoolId, Long id) {
        SchoolAcademicYear y = yearRepository.findById(id)
                .orElseThrow(() -> new AcademicYearNotFoundException(id));
        if (!y.getSchoolId().equals(schoolId)) throw new AcademicYearNotFoundException(id);
        return y;
    }

    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new ValidationException("End date must be after start date");
        }
    }

    private void validateNoOverlap(Long schoolId, Long excludeId,
                                    LocalDate startDate, LocalDate endDate) {
        // For new records we pass Long.MIN_VALUE which will never match any DB id
        Long safeExclude = excludeId == Long.MIN_VALUE ? -1L : excludeId;
        if (!yearRepository.findOverlapping(schoolId, safeExclude, startDate, endDate).isEmpty()) {
            throw new ValidationException(
                    "Academic year date range overlaps with an existing non-CANCELLED year");
        }
    }
}
