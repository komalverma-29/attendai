package com.attendai.school.section.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.section.dto.ChangeSectionStatusRequest;
import com.attendai.school.section.dto.CreateSectionRequest;
import com.attendai.school.section.dto.EnrollStudentInSectionRequest;
import com.attendai.school.section.entity.SchoolSection;
import com.attendai.school.section.entity.SectionEnrollment;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.exception.SectionEnrollmentNotFoundException;
import com.attendai.school.section.exception.SectionNotFoundException;
import com.attendai.school.section.mapper.SchoolSectionMapper;
import com.attendai.school.section.repository.SchoolSectionRepository;
import com.attendai.school.section.repository.SectionEnrollmentRepository;
import com.attendai.school.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSectionServiceImplTest {

    @Mock SchoolSectionRepository    sectionRepository;
    @Mock SectionEnrollmentRepository enrollmentRepository;
    @Mock SchoolSectionMapper        sectionMapper;
    @Mock AcademicYearService        academicYearService;
    @Mock SchoolClassService         classService;
    @Mock StudentService             studentService;
    @Mock AuditService               auditService;

    private SchoolSectionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SchoolSectionServiceImpl(
                sectionRepository, enrollmentRepository, sectionMapper,
                academicYearService, classService, studentService, auditService);
    }

    // -------------------------------------------------------------------------
    // createSection
    // -------------------------------------------------------------------------

    @Test
    void createSection_shouldSave_whenValidRequest() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(classService.findByIdOrThrow(5L)).thenReturn(buildClassResponse(5L, 1L));
        when(sectionRepository.existsByClassIdAndAcademicYearIdAndName(5L, 10L, "A"))
                .thenReturn(false);
        SchoolSection saved = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.save(any())).thenReturn(saved);
        when(sectionMapper.toResponse(saved)).thenReturn(null);

        service.createSection(1L, 10L, 5L, buildCreateRequest("A"));

        verify(sectionRepository).save(any(SchoolSection.class));
        verify(auditService).log(any());
    }

    @Test
    void createSection_shouldThrow_whenAcademicYearCompleted() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.COMPLETED));

        assertThatThrownBy(() -> service.createSection(1L, 10L, 5L, buildCreateRequest("A")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createSection_shouldThrow_whenAcademicYearCancelled() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.CANCELLED));

        assertThatThrownBy(() -> service.createSection(1L, 10L, 5L, buildCreateRequest("A")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void createSection_shouldThrow_whenClassBelongsToDifferentSchool() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(classService.findByIdOrThrow(5L)).thenReturn(buildClassResponse(5L, 99L)); // school 99

        assertThatThrownBy(() -> service.createSection(1L, 10L, 5L, buildCreateRequest("A")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
    }

    @Test
    void createSection_shouldThrow409_whenNameDuplicate() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(classService.findByIdOrThrow(5L)).thenReturn(buildClassResponse(5L, 1L));
        when(sectionRepository.existsByClassIdAndAcademicYearIdAndName(5L, 10L, "A"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSection(1L, 10L, 5L, buildCreateRequest("A")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createSection_shouldAllow_whenYearIsUpcoming() {
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.UPCOMING));
        when(classService.findByIdOrThrow(5L)).thenReturn(buildClassResponse(5L, 1L));
        when(sectionRepository.existsByClassIdAndAcademicYearIdAndName(5L, 10L, "A"))
                .thenReturn(false);
        SchoolSection saved = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.save(any())).thenReturn(saved);
        when(sectionMapper.toResponse(any())).thenReturn(null);

        service.createSection(1L, 10L, 5L, buildCreateRequest("A"));

        verify(sectionRepository).save(any(SchoolSection.class));
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenSectionBelongsToDifferentSchool() {
        SchoolSection s = buildSection(1L, 2L, 5L, 10L, "A", SectionStatus.ACTIVE); // school 2
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(SectionNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenSectionNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(SectionNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateSection
    // -------------------------------------------------------------------------

    @Test
    void updateSection_shouldThrow409_whenNewNameAlreadyExists() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sectionRepository.existsByClassIdAndAcademicYearIdAndName(5L, 10L, "B"))
                .thenReturn(true);

        com.attendai.school.section.dto.UpdateSectionRequest req =
                new com.attendai.school.section.dto.UpdateSectionRequest();
        req.setName("B");

        assertThatThrownBy(() -> service.updateSection(1L, 1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldSetInactive() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sectionRepository.save(any())).thenReturn(s);
        when(sectionMapper.toResponse(any())).thenReturn(null);

        ChangeSectionStatusRequest req = new ChangeSectionStatusRequest();
        req.setStatus(SectionStatus.INACTIVE);
        service.changeStatus(1L, 1L, req);

        assertThat(s.getStatus()).isEqualTo(SectionStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // deleteSection
    // -------------------------------------------------------------------------

    @Test
    void deleteSection_shouldSoftDelete_whenNoEnrollments() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(1L)).thenReturn(0L);
        when(sectionRepository.save(any())).thenReturn(s);

        service.deleteSection(1L, 1L);

        assertThat(s.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    @Test
    void deleteSection_shouldThrow_whenStudentsEnrolled() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteSection(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("enrolled student");
    }

    // -------------------------------------------------------------------------
    // enrollStudent
    // -------------------------------------------------------------------------

    @Test
    void enrollStudent_shouldSave_whenAllRulesPass() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(studentService.isActive(20L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndAcademicYearId(20L, 10L)).thenReturn(false);
        when(enrollmentRepository.existsBySectionIdAndAcademicYearIdAndRollNumber(1L, 10L, "01"))
                .thenReturn(false);

        SectionEnrollment saved = buildEnrollment(1L, 1L, 20L, 10L, "01");
        when(enrollmentRepository.save(any())).thenReturn(saved);
        when(sectionMapper.toEnrollmentResponse(saved)).thenReturn(null);

        service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01"));

        verify(enrollmentRepository).save(any(SectionEnrollment.class));
        verify(auditService).log(any());
    }

    @Test
    void enrollStudent_shouldThrow_whenSectionInactive() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.INACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("INACTIVE");
    }

    @Test
    void enrollStudent_shouldThrow_whenAcademicYearCompleted() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.COMPLETED));

        assertThatThrownBy(() -> service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void enrollStudent_shouldThrow_whenStudentNotActive() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(studentService.isActive(20L)).thenReturn(false);

        assertThatThrownBy(() -> service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not ACTIVE");
    }

    @Test
    void enrollStudent_shouldThrow409_whenStudentAlreadyEnrolledInAYear() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(studentService.isActive(20L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndAcademicYearId(20L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void enrollStudent_shouldThrow409_whenRollNumberTaken() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(academicYearService.findById(1L, 10L)).thenReturn(
                buildYearResponse(10L, 1L, AcademicYearStatus.ACTIVE));
        when(studentService.isActive(20L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndAcademicYearId(20L, 10L)).thenReturn(false);
        when(enrollmentRepository.existsBySectionIdAndAcademicYearIdAndRollNumber(1L, 10L, "01"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.enrollStudent(1L, 1L, buildEnrollRequest(20L, "01")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // removeStudent
    // -------------------------------------------------------------------------

    @Test
    void removeStudent_shouldDelete_whenEnrolled() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        SectionEnrollment e = buildEnrollment(1L, 1L, 20L, 10L, "01");
        when(enrollmentRepository.findBySectionIdAndStudentId(1L, 20L))
                .thenReturn(Optional.of(e));

        service.removeStudent(1L, 1L, 20L);

        verify(enrollmentRepository).delete(e);
        verify(auditService).log(any());
    }

    @Test
    void removeStudent_shouldThrow_whenNotEnrolled() {
        SchoolSection s = buildSection(1L, 1L, 5L, 10L, "A", SectionStatus.ACTIVE);
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.findBySectionIdAndStudentId(1L, 20L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeStudent(1L, 1L, 20L))
                .isInstanceOf(SectionEnrollmentNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void isStudentEnrolledInSection_shouldReturnTrue_whenEnrolled() {
        when(enrollmentRepository.existsBySectionIdAndStudentIdAndAcademicYearId(1L, 20L, 10L))
                .thenReturn(true);
        assertThat(service.isStudentEnrolledInSection(20L, 1L, 10L)).isTrue();
    }

    @Test
    void getStudentsBySection_shouldReturnOrderedList() {
        SectionEnrollment e1 = buildEnrollment(1L, 1L, 20L, 10L, "01");
        SectionEnrollment e2 = buildEnrollment(2L, 1L, 21L, 10L, "02");
        when(enrollmentRepository.findBySectionIdOrderByRollNumberAsc(1L))
                .thenReturn(List.of(e1, e2));
        when(sectionMapper.toEnrollmentResponse(any())).thenReturn(null);

        List<?> results = service.getStudentsBySection(1L);
        assertThat(results).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolSection buildSection(Long id, Long schoolId, Long classId,
                                        Long academicYearId, String name,
                                        SectionStatus status) {
        SchoolSection s = SchoolSection.builder()
                .schoolId(schoolId).classId(classId).academicYearId(academicYearId)
                .name(name).status(status).build();
        s.setId(id);
        return s;
    }

    private SectionEnrollment buildEnrollment(Long id, Long sectionId, Long studentId,
                                               Long academicYearId, String rollNumber) {
        SectionEnrollment e = SectionEnrollment.builder()
                .sectionId(sectionId).studentId(studentId)
                .academicYearId(academicYearId).rollNumber(rollNumber)
                .enrolledAt(LocalDate.now()).build();
        e.setId(id);
        return e;
    }

    private AcademicYearResponse buildYearResponse(Long id, Long schoolId,
                                                    AcademicYearStatus status) {
        return AcademicYearResponse.builder()
                .id(id).schoolId(schoolId).name("2025-2026")
                .startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private ClassResponse buildClassResponse(Long classId, Long schoolId) {
        return ClassResponse.builder()
                .id(classId).schoolId(schoolId).name("Grade 5").gradeOrder(5)
                .status(ClassStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private CreateSectionRequest buildCreateRequest(String name) {
        CreateSectionRequest req = new CreateSectionRequest();
        req.setName(name);
        return req;
    }

    private EnrollStudentInSectionRequest buildEnrollRequest(Long studentId, String rollNumber) {
        EnrollStudentInSectionRequest req = new EnrollStudentInSectionRequest();
        req.setStudentId(studentId);
        req.setRollNumber(rollNumber);
        req.setEnrolledAt(LocalDate.now());
        return req;
    }
}
