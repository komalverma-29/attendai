package com.attendai.school.schoolclass.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.schoolclass.dto.ChangeClassStatusRequest;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.dto.ClassSummaryResponse;
import com.attendai.school.schoolclass.dto.CreateClassRequest;
import com.attendai.school.schoolclass.dto.UpdateClassRequest;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.entity.SchoolClass;
import com.attendai.school.schoolclass.exception.ClassNotFoundException;
import com.attendai.school.schoolclass.mapper.SchoolClassMapper;
import com.attendai.school.schoolclass.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolClassServiceImpl implements SchoolClassService {

    private static final String MODULE = "school";

    private final SchoolClassRepository classRepository;
    private final SchoolClassMapper     classMapper;
    private final SchoolService         schoolService;
    private final AuditService          auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ClassResponse createClass(Long schoolId, CreateClassRequest request) {
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        if (classRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Class '" + request.getName() + "' already exists in school " + schoolId);
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .displayName(request.getDisplayName())
                .gradeOrder(request.getGradeOrder())
                .status(ClassStatus.ACTIVE)
                .build();

        SchoolClass saved = classRepository.save(schoolClass);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CLASS_CREATED")
                .module(MODULE).resourceType("SchoolClass")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId + ",\"name\":\"" + request.getName() + "\"}")
                .build());

        return classMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ClassResponse findById(Long schoolId, Long id) {
        return classMapper.toResponse(requireClass(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSummaryResponse> listClasses(Long schoolId, ClassStatus status) {
        return classRepository.findBySchoolIdAndOptionalStatus(schoolId, status)
                .stream()
                .map(classMapper::toSummaryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ClassResponse updateClass(Long schoolId, Long id, UpdateClassRequest request) {
        SchoolClass schoolClass = requireClass(schoolId, id);

        if (request.getName() != null && !request.getName().equals(schoolClass.getName())) {
            if (classRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
                throw new ResourceAlreadyExistsException(
                        "Class '" + request.getName()
                        + "' already exists in school " + schoolId);
            }
            schoolClass.setName(request.getName());
        }
        if (request.getDisplayName() != null) {
            schoolClass.setDisplayName(request.getDisplayName());
        }
        if (request.getGradeOrder() != null) {
            schoolClass.setGradeOrder(request.getGradeOrder());
        }

        SchoolClass saved = classRepository.save(schoolClass);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CLASS_UPDATED")
                .module(MODULE).resourceType("SchoolClass")
                .resourceId(String.valueOf(id)).build());

        return classMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ClassResponse changeStatus(Long schoolId, Long id, ChangeClassStatusRequest request) {
        SchoolClass schoolClass = requireClass(schoolId, id);
        schoolClass.setStatus(request.getStatus());
        SchoolClass saved = classRepository.save(schoolClass);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CLASS_STATUS_CHANGED")
                .module(MODULE).resourceType("SchoolClass")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return classMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteClass(Long schoolId, Long id) {
        SchoolClass schoolClass = requireClass(schoolId, id);

        // BR-CLASS-04: class with sections cannot be deleted.
        // school-section is a future module — no sections possible yet.
        // Guard comment preserved for when school-section is implemented.

        schoolClass.softDelete();
        classRepository.save(schoolClass);

        auditService.log(AuditEventRequest.builder()
                .actionCode("CLASS_DELETED")
                .module(MODULE).resourceType("SchoolClass")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return classRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse findByIdOrThrow(Long id) {
        return classMapper.toResponse(
                classRepository.findById(id)
                        .orElseThrow(() -> new ClassNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return classRepository.findById(id)
                .map(c -> ClassStatus.ACTIVE.equals(c.getStatus()))
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolClass requireClass(Long schoolId, Long id) {
        SchoolClass c = classRepository.findById(id)
                .orElseThrow(() -> new ClassNotFoundException(id));
        if (!c.getSchoolId().equals(schoolId)) throw new ClassNotFoundException(id);
        return c;
    }
}
