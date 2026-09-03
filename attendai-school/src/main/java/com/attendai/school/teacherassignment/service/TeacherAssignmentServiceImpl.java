package com.attendai.school.teacherassignment.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.subject.repository.ClassSubjectRepository;
import com.attendai.school.subject.service.SchoolSubjectService;
import com.attendai.school.teacher.service.TeacherService;
import com.attendai.school.teacherassignment.dto.ChangeAssignmentStatusRequest;
import com.attendai.school.teacherassignment.dto.CreateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentResponse;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentSummaryResponse;
import com.attendai.school.teacherassignment.dto.UpdateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.entity.TeacherAssignment;
import com.attendai.school.teacherassignment.exception.TeacherAssignmentNotFoundException;
import com.attendai.school.teacherassignment.mapper.TeacherAssignmentMapper;
import com.attendai.school.teacherassignment.repository.TeacherAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private static final String MODULE = "school";

    private final TeacherAssignmentRepository assignmentRepository;
    private final TeacherAssignmentMapper     assignmentMapper;
    private final AcademicYearService         academicYearService;
    private final SchoolSectionService        sectionService;
    private final SchoolSubjectService        subjectService;
    private final SchoolClassService          classService;
    private final TeacherService              teacherService;
    private final ClassSubjectRepository      classSubjectRepository;
    private final AuditService                auditService;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public TeacherAssignmentResponse createAssignment(Long schoolId, Long academicYearId,
                                                       CreateTeacherAssignmentRequest request) {
        // Validate academic year belongs to this school
        var year = academicYearService.findById(schoolId, academicYearId);

        // Validate section belongs to this school
        var section = sectionService.findById(request.getSectionId());
        if (!section.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Section " + request.getSectionId() + " does not belong to school " + schoolId);
        }

        // BR-ASSIGN-04: section must be ACTIVE
        if (!com.attendai.school.section.entity.SectionStatus.ACTIVE
                .equals(section.getStatus())) {
            throw new ValidationException(
                    "Section " + request.getSectionId() + " is not ACTIVE");
        }

        // Validate subject belongs to this school
        var subject = subjectService.findByIdOrThrow(request.getSubjectId());
        if (!subject.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Subject " + request.getSubjectId() + " does not belong to school " + schoolId);
        }

        // BR-ASSIGN-03: subject must be associated with the section's class
        Long classId = section.getClassId();
        if (!classSubjectRepository.existsByClassIdAndSubjectId(classId, request.getSubjectId())) {
            throw new ValidationException(
                    "Subject " + request.getSubjectId()
                    + " is not associated with class " + classId);
        }

        // Validate teacher belongs to this school and is ACTIVE (BR-ASSIGN-02)
        var teacher = teacherService.findById(schoolId, request.getTeacherId());
        if (!teacherService.isActive(request.getTeacherId())) {
            throw new ValidationException(
                    "Teacher " + request.getTeacherId() + " is not ACTIVE");
        }

        // BR-ASSIGN-01: unique active assignment per subject+section+year
        if (assignmentRepository.existsBySectionIdAndSubjectIdAndAcademicYearId(
                request.getSectionId(), request.getSubjectId(), academicYearId)) {
            throw new ResourceAlreadyExistsException(
                    "An assignment already exists for subject " + request.getSubjectId()
                    + " in section " + request.getSectionId()
                    + " for academic year " + academicYearId);
        }

        // BR-ASSIGN-05: only one class teacher per section per year
        if (request.isClassTeacher()
                && assignmentRepository.existsBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(
                        request.getSectionId(), academicYearId)) {
            throw new ResourceAlreadyExistsException(
                    "A class teacher already exists for section " + request.getSectionId()
                    + " in academic year " + academicYearId);
        }

        TeacherAssignment assignment = TeacherAssignment.builder()
                .schoolId(schoolId)
                .academicYearId(academicYearId)
                .sectionId(request.getSectionId())
                .subjectId(request.getSubjectId())
                .teacherId(request.getTeacherId())
                .isClassTeacher(request.isClassTeacher())
                .status(AssignmentStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        TeacherAssignment saved = assignmentRepository.save(assignment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_ASSIGNMENT_CREATED")
                .module(MODULE).resourceType("TeacherAssignment")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"sectionId\":" + request.getSectionId()
                         + ",\"subjectId\":" + request.getSubjectId()
                         + ",\"teacherId\":" + request.getTeacherId() + "}")
                .build());

        return assignmentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TeacherAssignmentResponse findById(Long schoolId, Long id) {
        return assignmentMapper.toResponse(requireAssignment(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentSummaryResponse> listAssignments(Long schoolId,
                                                                   Long academicYearId,
                                                                   Long sectionId,
                                                                   Long teacherId,
                                                                   Long subjectId) {
        return assignmentRepository.findByFilters(schoolId, academicYearId,
                        sectionId, teacherId, subjectId)
                .stream()
                .map(assignmentMapper::toSummaryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public TeacherAssignmentResponse updateAssignment(Long schoolId, Long id,
                                                       UpdateTeacherAssignmentRequest request) {
        TeacherAssignment assignment = requireAssignment(schoolId, id);

        // Change teacher — must be ACTIVE and in the same school
        if (request.getTeacherId() != null) {
            teacherService.findById(schoolId, request.getTeacherId()); // validates school scope
            if (!teacherService.isActive(request.getTeacherId())) {
                throw new ValidationException(
                        "Teacher " + request.getTeacherId() + " is not ACTIVE");
            }
            assignment.setTeacherId(request.getTeacherId());
        }

        // Update class-teacher flag — BR-ASSIGN-05 check
        if (request.getClassTeacher() != null) {
            if (Boolean.TRUE.equals(request.getClassTeacher()) && !assignment.isClassTeacher()) {
                // Only block if a different assignment already holds the flag
                assignmentRepository
                        .findBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(
                                assignment.getSectionId(), assignment.getAcademicYearId())
                        .ifPresent(existing -> {
                            if (!existing.getId().equals(id)) {
                                throw new ResourceAlreadyExistsException(
                                        "A class teacher already exists for section "
                                        + assignment.getSectionId());
                            }
                        });
            }
            assignment.setClassTeacher(request.getClassTeacher());
        }

        if (request.getNotes() != null) {
            assignment.setNotes(request.getNotes());
        }

        TeacherAssignment saved = assignmentRepository.save(assignment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_ASSIGNMENT_UPDATED")
                .module(MODULE).resourceType("TeacherAssignment")
                .resourceId(String.valueOf(id)).build());

        return assignmentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public TeacherAssignmentResponse changeStatus(Long schoolId, Long id,
                                                   ChangeAssignmentStatusRequest request) {
        TeacherAssignment assignment = requireAssignment(schoolId, id);
        assignment.setStatus(request.getStatus());
        TeacherAssignment saved = assignmentRepository.save(assignment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_ASSIGNMENT_STATUS_CHANGED")
                .module(MODULE).resourceType("TeacherAssignment")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return assignmentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteAssignment(Long schoolId, Long id) {
        TeacherAssignment assignment = requireAssignment(schoolId, id);

        // FR-ASSIGN-07: rejected if timetable entries reference this assignment.
        // school-timetable is the next feature (Round 3B) — guard documented.
        // Will be enforced when TimetableEntryRepository is available.

        assignment.softDelete();
        assignmentRepository.save(assignment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_ASSIGNMENT_DELETED")
                .module(MODULE).resourceType("TeacherAssignment")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignmentsForSection(Long sectionId,
                                                                     Long academicYearId) {
        return assignmentRepository.findBySectionIdAndAcademicYearId(sectionId, academicYearId)
                .stream()
                .map(assignmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeacherAssignmentResponse> getClassTeacherForSection(Long sectionId,
                                                                          Long academicYearId) {
        return assignmentRepository
                .findBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(sectionId, academicYearId)
                .map(assignmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return assignmentRepository.existsById(id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TeacherAssignment requireAssignment(Long schoolId, Long id) {
        TeacherAssignment a = assignmentRepository.findById(id)
                .orElseThrow(() -> new TeacherAssignmentNotFoundException(id));
        if (!a.getSchoolId().equals(schoolId))
            throw new TeacherAssignmentNotFoundException(id);
        return a;
    }
}
