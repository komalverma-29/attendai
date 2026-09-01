package com.attendai.school.section.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.section.dto.ChangeSectionStatusRequest;
import com.attendai.school.section.dto.CreateSectionRequest;
import com.attendai.school.section.dto.EnrollStudentInSectionRequest;
import com.attendai.school.section.dto.SectionEnrollmentResponse;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.dto.SectionSummaryResponse;
import com.attendai.school.section.dto.UpdateSectionRequest;
import com.attendai.school.section.entity.SchoolSection;
import com.attendai.school.section.entity.SectionEnrollment;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.exception.SectionEnrollmentNotFoundException;
import com.attendai.school.section.exception.SectionNotFoundException;
import com.attendai.school.section.mapper.SchoolSectionMapper;
import com.attendai.school.section.repository.SchoolSectionRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolSectionServiceImpl implements SchoolSectionService {

    private static final String MODULE = "school";

    private final SchoolSectionRepository    sectionRepository;
    private final SectionEnrollmentRepository enrollmentRepository;
    private final SchoolSectionMapper        sectionMapper;
    private final AcademicYearService        academicYearService;
    private final SchoolClassService         classService;
    private final StudentService             studentService;
    private final AuditService               auditService;

    // -------------------------------------------------------------------------
    // Create Section
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SectionResponse createSection(Long schoolId, Long academicYearId, Long classId,
                                          CreateSectionRequest request) {
        // Validate academic year belongs to school and is not COMPLETED/CANCELLED
        validateAcademicYear(schoolId, academicYearId);

        // Validate class belongs to school
        var classResponse = classService.findByIdOrThrow(classId);
        if (!classResponse.getSchoolId().equals(schoolId)) {
            throw new ValidationException(
                    "Class " + classId + " does not belong to school " + schoolId);
        }

        // BR-SEC-01: section name unique within class+year
        if (sectionRepository.existsByClassIdAndAcademicYearIdAndName(
                classId, academicYearId, request.getName())) {
            throw new ResourceAlreadyExistsException(
                    "Section '" + request.getName()
                    + "' already exists for class " + classId
                    + " in academic year " + academicYearId);
        }

        SchoolSection section = SchoolSection.builder()
                .schoolId(schoolId)
                .classId(classId)
                .academicYearId(academicYearId)
                .name(request.getName())
                .description(request.getDescription())
                .status(SectionStatus.ACTIVE)
                .build();

        SchoolSection saved = sectionRepository.save(section);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SECTION_CREATED")
                .module(MODULE).resourceType("SchoolSection")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"classId\":" + classId
                         + ",\"academicYearId\":" + academicYearId
                         + ",\"name\":\"" + request.getName() + "\"}")
                .build());

        return sectionMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SectionResponse findById(Long schoolId, Long sectionId) {
        return sectionMapper.toResponse(requireSection(schoolId, sectionId));
    }

    /** Internal API — no schoolId scope check (for inter-module use). */
    @Override
    @Transactional(readOnly = true)
    public SectionResponse findById(Long sectionId) {
        return sectionMapper.toResponse(
                sectionRepository.findById(sectionId)
                        .orElseThrow(() -> new SectionNotFoundException(sectionId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionSummaryResponse> listSections(Long schoolId, Long academicYearId,
                                                      Long classId) {
        return sectionRepository.findByClassIdAndAcademicYearId(classId, academicYearId)
                .stream()
                .filter(s -> s.getSchoolId().equals(schoolId))
                .map(s -> SectionSummaryResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .description(s.getDescription())
                        .status(s.getStatus())
                        .studentCount(enrollmentRepository.countBySectionId(s.getId()))
                        .build())
                .toList();
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SectionResponse updateSection(Long schoolId, Long sectionId,
                                          UpdateSectionRequest request) {
        SchoolSection section = requireSection(schoolId, sectionId);

        if (request.getName() != null && !request.getName().equals(section.getName())) {
            // BR-SEC-01: name must remain unique within class+year
            if (sectionRepository.existsByClassIdAndAcademicYearIdAndName(
                    section.getClassId(), section.getAcademicYearId(), request.getName())) {
                throw new ResourceAlreadyExistsException(
                        "Section '" + request.getName()
                        + "' already exists for class " + section.getClassId()
                        + " in academic year " + section.getAcademicYearId());
            }
            section.setName(request.getName());
        }
        if (request.getDescription() != null) {
            section.setDescription(request.getDescription());
        }

        SchoolSection saved = sectionRepository.save(section);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SECTION_UPDATED")
                .module(MODULE).resourceType("SchoolSection")
                .resourceId(String.valueOf(sectionId)).build());

        return sectionMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SectionResponse changeStatus(Long schoolId, Long sectionId,
                                         ChangeSectionStatusRequest request) {
        SchoolSection section = requireSection(schoolId, sectionId);
        section.setStatus(request.getStatus());
        SchoolSection saved = sectionRepository.save(section);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SECTION_STATUS_CHANGED")
                .module(MODULE).resourceType("SchoolSection")
                .resourceId(String.valueOf(sectionId))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return sectionMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteSection(Long schoolId, Long sectionId) {
        SchoolSection section = requireSection(schoolId, sectionId);

        // BR-SEC-07: reject if students are enrolled
        long enrollmentCount = enrollmentRepository.countBySectionId(sectionId);
        if (enrollmentCount > 0) {
            throw new ValidationException(
                    "Cannot delete section " + sectionId
                    + " — it has " + enrollmentCount + " enrolled student(s)");
        }

        section.softDelete();
        sectionRepository.save(section);

        auditService.log(AuditEventRequest.builder()
                .actionCode("SECTION_DELETED")
                .module(MODULE).resourceType("SchoolSection")
                .resourceId(String.valueOf(sectionId)).build());
    }

    // -------------------------------------------------------------------------
    // Student enrollment
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SectionEnrollmentResponse enrollStudent(Long schoolId, Long sectionId,
                                                    EnrollStudentInSectionRequest request) {
        SchoolSection section = requireSection(schoolId, sectionId);

        // BR-SEC-05: INACTIVE section does not accept new enrollments
        if (SectionStatus.INACTIVE.equals(section.getStatus())) {
            throw new ValidationException(
                    "Section " + sectionId + " is INACTIVE and does not accept new enrollments");
        }

        // BR-SEC-06: academic year must not be COMPLETED or CANCELLED
        validateAcademicYearForEnrollment(schoolId, section.getAcademicYearId());

        // BR-SEC-04: student must be ACTIVE
        if (!studentService.isActive(request.getStudentId())) {
            throw new ValidationException(
                    "Student " + request.getStudentId() + " is not ACTIVE");
        }

        // BR-SEC-02: one section per student per academic year
        if (enrollmentRepository.existsByStudentIdAndAcademicYearId(
                request.getStudentId(), section.getAcademicYearId())) {
            throw new ResourceAlreadyExistsException(
                    "Student " + request.getStudentId()
                    + " is already enrolled in a section for academic year "
                    + section.getAcademicYearId());
        }

        // BR-SEC-03: roll number unique within section+year
        if (enrollmentRepository.existsBySectionIdAndAcademicYearIdAndRollNumber(
                sectionId, section.getAcademicYearId(), request.getRollNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Roll number '" + request.getRollNumber()
                    + "' is already assigned in section " + sectionId);
        }

        SectionEnrollment enrollment = SectionEnrollment.builder()
                .sectionId(sectionId)
                .studentId(request.getStudentId())
                .academicYearId(section.getAcademicYearId())
                .rollNumber(request.getRollNumber())
                .enrolledAt(request.getEnrolledAt())
                .build();

        SectionEnrollment saved = enrollmentRepository.save(enrollment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_ENROLLED_IN_SECTION")
                .module(MODULE).resourceType("SectionEnrollment")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"sectionId\":" + sectionId
                         + ",\"studentId\":" + request.getStudentId()
                         + ",\"rollNumber\":\"" + request.getRollNumber() + "\"}")
                .build());

        return sectionMapper.toEnrollmentResponse(saved);
    }

    @Override
    @Transactional
    public void removeStudent(Long schoolId, Long sectionId, Long studentId) {
        requireSection(schoolId, sectionId);

        SectionEnrollment enrollment = enrollmentRepository
                .findBySectionIdAndStudentId(sectionId, studentId)
                .orElseThrow(() -> new SectionEnrollmentNotFoundException(studentId, sectionId));

        // FR-SEC-07: guard against existing attendance records.
        // school-daily-attendance is a future module — guard is documented.

        enrollmentRepository.delete(enrollment);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_REMOVED_FROM_SECTION")
                .module(MODULE).resourceType("SectionEnrollment")
                .resourceId(String.valueOf(enrollment.getId()))
                .details("{\"sectionId\":" + sectionId + ",\"studentId\":" + studentId + "}")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionEnrollmentResponse> getStudentsBySection(Long sectionId) {
        return enrollmentRepository.findBySectionIdOrderByRollNumberAsc(sectionId)
                .stream()
                .map(sectionMapper::toEnrollmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SectionEnrollmentResponse> findStudentEnrollment(Long studentId,
                                                                       Long academicYearId) {
        return enrollmentRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId)
                .map(sectionMapper::toEnrollmentResponse);
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean isStudentEnrolledInSection(Long studentId, Long sectionId,
                                               Long academicYearId) {
        return enrollmentRepository.existsBySectionIdAndStudentIdAndAcademicYearId(
                sectionId, studentId, academicYearId);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolSection requireSection(Long schoolId, Long sectionId) {
        SchoolSection s = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new SectionNotFoundException(sectionId));
        if (!s.getSchoolId().equals(schoolId)) throw new SectionNotFoundException(sectionId);
        return s;
    }

    /**
     * Validates that the academic year exists, belongs to the school,
     * and is not in a terminal (COMPLETED/CANCELLED) state for creation.
     * UPCOMING and ACTIVE years are both valid for section creation.
     */
    private void validateAcademicYear(Long schoolId, Long academicYearId) {
        var yearResponse = academicYearService.findById(schoolId, academicYearId);
        if (AcademicYearStatus.COMPLETED.equals(yearResponse.getStatus())
                || AcademicYearStatus.CANCELLED.equals(yearResponse.getStatus())) {
            throw new ValidationException(
                    "Cannot create a section for a " + yearResponse.getStatus()
                    + " academic year");
        }
    }

    /**
     * BR-SEC-06: enrollment is only allowed when the academic year is ACTIVE or UPCOMING.
     */
    private void validateAcademicYearForEnrollment(Long schoolId, Long academicYearId) {
        var yearResponse = academicYearService.findById(schoolId, academicYearId);
        if (AcademicYearStatus.COMPLETED.equals(yearResponse.getStatus())
                || AcademicYearStatus.CANCELLED.equals(yearResponse.getStatus())) {
            throw new ValidationException(
                    "Cannot enroll students in a section for a "
                    + yearResponse.getStatus() + " academic year");
        }
    }
}
