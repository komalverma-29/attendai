package com.attendai.school.subject.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.subject.dto.AssignSubjectToClassRequest;
import com.attendai.school.subject.dto.ChangeSubjectStatusRequest;
import com.attendai.school.subject.dto.CreateSubjectRequest;
import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.dto.SubjectSummaryResponse;
import com.attendai.school.subject.dto.UpdateSubjectRequest;
import com.attendai.school.subject.entity.ClassSubject;
import com.attendai.school.subject.entity.SchoolSubject;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import com.attendai.school.subject.exception.SubjectNotFoundException;
import com.attendai.school.subject.mapper.SchoolSubjectMapper;
import com.attendai.school.subject.repository.ClassSubjectRepository;
import com.attendai.school.subject.repository.SchoolSubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolSubjectServiceImpl implements SchoolSubjectService {

    private static final String MODULE = "school";

    private final SchoolSubjectRepository subjectRepository;
    private final ClassSubjectRepository  classSubjectRepository;
    private final SchoolSubjectMapper     subjectMapper;
    private final SchoolService           schoolService;
    private final SchoolClassService      classService;
    private final AuditService            auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SubjectResponse createSubject(Long schoolId, CreateSubjectRequest request) {
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        if (subjectRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Subject name '" + request.getName()
                    + "' already exists in school " + schoolId);
        }
        if (subjectRepository.existsBySchoolIdAndCode(schoolId, request.getCode())) {
            throw new ResourceAlreadyExistsException(
                    "Subject code '" + request.getCode()
                    + "' already exists in school " + schoolId);
        }

        SchoolSubject subject = SchoolSubject.builder()
                .schoolId(schoolId)
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .type(request.getType())
                .description(request.getDescription())
                .status(SubjectStatus.ACTIVE)
                .build();

        SchoolSubject saved = subjectRepository.save(subject);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_CREATED")
                .module(MODULE).resourceType("SchoolSubject")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"code\":\"" + saved.getCode() + "\"}")
                .build());

        return subjectMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse findById(Long schoolId, Long id) {
        return subjectMapper.toResponse(requireSubject(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectSummaryResponse> listSubjects(Long schoolId, SubjectType type,
                                                      SubjectStatus status, Long classId) {
        if (classId != null) {
            // Filter by class — only subjects linked to that class, scoped to school
            return subjectRepository.findByClassId(classId)
                    .stream()
                    .filter(s -> s.getSchoolId().equals(schoolId))
                    .filter(s -> type == null || s.getType() == type)
                    .filter(s -> status == null || s.getStatus() == status)
                    .map(subjectMapper::toSummaryResponse)
                    .toList();
        }
        return subjectRepository.findBySchoolIdAndFilters(schoolId, type, status)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SubjectResponse updateSubject(Long schoolId, Long id, UpdateSubjectRequest request) {
        SchoolSubject subject = requireSubject(schoolId, id);

        if (request.getName() != null && !request.getName().equals(subject.getName())) {
            if (subjectRepository.existsBySchoolIdAndName(schoolId, request.getName())) {
                throw new ResourceAlreadyExistsException(
                        "Subject name '" + request.getName()
                        + "' already exists in school " + schoolId);
            }
            subject.setName(request.getName());
        }
        if (request.getType() != null)        subject.setType(request.getType());
        if (request.getDescription() != null) subject.setDescription(request.getDescription());

        SchoolSubject saved = subjectRepository.save(subject);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_UPDATED")
                .module(MODULE).resourceType("SchoolSubject")
                .resourceId(String.valueOf(id)).build());

        return subjectMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SubjectResponse changeStatus(Long schoolId, Long id,
                                         ChangeSubjectStatusRequest request) {
        SchoolSubject subject = requireSubject(schoolId, id);
        subject.setStatus(request.getStatus());
        SchoolSubject saved = subjectRepository.save(subject);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_STATUS_CHANGED")
                .module(MODULE).resourceType("SchoolSubject")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return subjectMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Class assignment
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void assignToClass(Long schoolId, Long subjectId,
                               AssignSubjectToClassRequest request) {
        SchoolSubject subject = requireSubject(schoolId, subjectId);
        Long classId = request.getClassId();

        // Verify class exists and belongs to the same school
        var classResponse = classService.findByIdOrThrow(classId);
        if (!classResponse.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Class with id " + classId + " does not belong to school " + schoolId);
        }
        if (classSubjectRepository.existsByClassIdAndSubjectId(classId, subjectId)) {
            throw new ResourceAlreadyExistsException(
                    "Subject " + subjectId + " is already assigned to class " + classId);
        }

        ClassSubject cs = ClassSubject.builder()
                .classId(classId).subjectId(subjectId).build();
        classSubjectRepository.save(cs);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_ASSIGNED_TO_CLASS")
                .module(MODULE).resourceType("ClassSubject")
                .resourceId(String.valueOf(subjectId))
                .details("{\"classId\":" + classId + "}")
                .build());
    }

    @Override
    @Transactional
    public void removeFromClass(Long schoolId, Long subjectId, Long classId) {
        requireSubject(schoolId, subjectId);

        // BR-SUB-05: guard against active teacher assignments — future module; documented
        ClassSubject cs = classSubjectRepository
                .findByClassIdAndSubjectId(classId, subjectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subject " + subjectId + " is not assigned to class " + classId));

        classSubjectRepository.delete(cs);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_REMOVED_FROM_CLASS")
                .module(MODULE).resourceType("ClassSubject")
                .resourceId(String.valueOf(subjectId))
                .details("{\"classId\":" + classId + "}")
                .build());
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteSubject(Long schoolId, Long id) {
        SchoolSubject subject = requireSubject(schoolId, id);

        // BR-SUB-05: guard against active teacher assignments / timetable entries.
        // school-teacher-assignment and school-timetable are future modules.
        // Guard is documented; will be enforced when those modules are built.

        subject.softDelete();
        subjectRepository.save(subject);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SUBJECT_DELETED")
                .module(MODULE).resourceType("SchoolSubject")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SubjectResponse findByIdOrThrow(Long subjectId) {
        return subjectMapper.toResponse(
                subjectRepository.findById(subjectId)
                        .orElseThrow(() -> new SubjectNotFoundException(subjectId)));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long subjectId) {
        return subjectRepository.existsById(subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectSummaryResponse> getSubjectsByClassId(Long classId) {
        return subjectRepository.findByClassId(classId)
                .stream()
                .map(subjectMapper::toSummaryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolSubject requireSubject(Long schoolId, Long id) {
        SchoolSubject s = subjectRepository.findById(id)
                .orElseThrow(() -> new SubjectNotFoundException(id));
        if (!s.getSchoolId().equals(schoolId)) throw new SubjectNotFoundException(id);
        return s;
    }
}
