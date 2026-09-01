package com.attendai.school.school.service;

import com.attendai.school.school.dto.ChangeSchoolStatusRequest;
import com.attendai.school.school.dto.CreateSchoolRequest;
import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.dto.SchoolSummaryResponse;
import com.attendai.school.school.dto.UpdateSchoolRequest;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * School management service.
 *
 * Exposes HTTP-facing CRUD operations and an internal API consumed by
 * all other school sub-modules to validate school existence and status.
 */
public interface SchoolService {

    // HTTP-facing operations
    SchoolResponse createSchool(CreateSchoolRequest request);
    SchoolResponse findById(Long id);
    Page<SchoolSummaryResponse> listSchools(SchoolStatus status, SchoolType type,
                                             String search, Pageable pageable);
    SchoolResponse updateSchool(Long id, UpdateSchoolRequest request);
    SchoolResponse changeStatus(Long id, ChangeSchoolStatusRequest request);
    void deleteSchool(Long id);

    // Internal API for other school sub-modules
    boolean existsById(Long id);
    boolean isActive(Long id);
    SchoolResponse findByIdOrThrow(Long id);
}
