package com.attendai.school.school.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.dto.ChangeSchoolStatusRequest;
import com.attendai.school.school.dto.CreateSchoolRequest;
import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.dto.SchoolSummaryResponse;
import com.attendai.school.school.dto.UpdateSchoolRequest;
import com.attendai.school.school.entity.School;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import com.attendai.school.school.exception.SchoolAlreadyExistsException;
import com.attendai.school.school.exception.SchoolNotFoundException;
import com.attendai.school.school.mapper.SchoolMapper;
import com.attendai.school.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private static final String MODULE = "school";

    /** Allowed status transitions: key = current, value = valid targets. */
    private static final Map<SchoolStatus, Set<SchoolStatus>> ALLOWED_TRANSITIONS = Map.of(
            SchoolStatus.ACTIVE,    EnumSet.of(SchoolStatus.INACTIVE, SchoolStatus.SUSPENDED),
            SchoolStatus.INACTIVE,  EnumSet.of(SchoolStatus.ACTIVE),
            SchoolStatus.SUSPENDED, EnumSet.of(SchoolStatus.ACTIVE)
    );

    private final SchoolRepository schoolRepository;
    private final SchoolMapper     schoolMapper;
    private final AuditService     auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SchoolResponse createSchool(CreateSchoolRequest request) {
        if (schoolRepository.existsByName(request.getName())) {
            throw new SchoolAlreadyExistsException("name", request.getName());
        }

        String code = resolveCode(request);

        if (schoolRepository.existsByCode(code)) {
            throw new SchoolAlreadyExistsException("code", code);
        }

        School school = School.builder()
                .name(request.getName().trim())
                .code(code)
                .type(request.getType())
                .status(SchoolStatus.ACTIVE)
                .description(request.getDescription())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .stateOrProvince(request.getStateOrProvince())
                .postalCode(request.getPostalCode())
                .country(request.getCountry().toUpperCase())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .build();

        School saved = schoolRepository.save(school);
        log.info("School created | schoolId={} name={}", saved.getId(), saved.getName());

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_CREATED")
                .module(MODULE)
                .resourceType("School")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"name\":\"" + saved.getName() + "\",\"code\":\"" + saved.getCode() + "\"}")
                .build());

        return schoolMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse findById(Long id) {
        return schoolMapper.toResponse(requireSchool(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse findByIdOrThrow(Long id) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return schoolRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return schoolRepository.findById(id)
                .map(s -> s.getStatus() == SchoolStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SchoolSummaryResponse> listSchools(SchoolStatus status, SchoolType type,
                                                    String search, Pageable pageable) {
        String normSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return schoolRepository.findByFilters(status, type, normSearch, pageable)
                .map(schoolMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SchoolResponse updateSchool(Long id, UpdateSchoolRequest request) {
        School school = requireSchool(id);

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equals(school.getName()) && schoolRepository.existsByName(newName)) {
                throw new SchoolAlreadyExistsException("name", newName);
            }
            school.setName(newName);
        }
        if (request.getType()            != null) school.setType(request.getType());
        if (request.getDescription()     != null) school.setDescription(request.getDescription());
        if (request.getAddressLine1()    != null) school.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2()    != null) school.setAddressLine2(request.getAddressLine2());
        if (request.getCity()            != null) school.setCity(request.getCity());
        if (request.getStateOrProvince() != null) school.setStateOrProvince(request.getStateOrProvince());
        if (request.getPostalCode()      != null) school.setPostalCode(request.getPostalCode());
        if (request.getCountry()         != null) school.setCountry(request.getCountry().toUpperCase());
        if (request.getPhone()           != null) school.setPhone(request.getPhone());
        if (request.getEmail()           != null) school.setEmail(request.getEmail());
        if (request.getWebsite()         != null) school.setWebsite(request.getWebsite());
        if (request.getLogoFileId()      != null) school.setLogoFileId(request.getLogoFileId());

        School saved = schoolRepository.save(school);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_UPDATED")
                .module(MODULE)
                .resourceType("School")
                .resourceId(String.valueOf(id))
                .build());

        return schoolMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SchoolResponse changeStatus(Long id, ChangeSchoolStatusRequest request) {
        School school = requireSchool(id);
        SchoolStatus from = school.getStatus();
        SchoolStatus to   = request.getStatus();

        Set<SchoolStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ValidationException(
                    "Cannot transition school status from " + from + " to " + to);
        }

        school.setStatus(to);
        School saved = schoolRepository.save(school);

        log.info("School status changed {} → {} | schoolId={}", from, to, id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_STATUS_CHANGED")
                .module(MODULE)
                .resourceType("School")
                .resourceId(String.valueOf(id))
                .details("{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}")
                .build());

        return schoolMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Soft delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteSchool(Long id) {
        School school = requireSchool(id);
        school.softDelete();
        schoolRepository.save(school);
        log.info("School soft-deleted | schoolId={}", id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SCHOOL_DELETED")
                .module(MODULE)
                .resourceType("School")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private School requireSchool(Long id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new SchoolNotFoundException(id));
    }

    /**
     * Generates a school code from the first letters of each word in the school name
     * if no explicit code was provided in the request.
     * The generated code is uppercase, max 10 characters.
     */
    private String resolveCode(CreateSchoolRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()) {
            return request.getCode().toUpperCase().trim();
        }
        // Auto-generate: take the first letter of each word, uppercase, max 10 chars
        String[] words = request.getName().trim().toUpperCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && sb.length() < 10) {
                sb.append(word.charAt(0));
            }
        }
        String candidate = sb.toString();
        // If the generated code is too short (e.g. single word), use first 6 chars of the name
        if (candidate.length() < 2) {
            candidate = request.getName().toUpperCase().replaceAll("[^A-Z0-9]", "");
            candidate = candidate.substring(0, Math.min(candidate.length(), 6));
        }
        return candidate;
    }
}
