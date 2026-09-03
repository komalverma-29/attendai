package com.attendai.school.teacherassignment.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.academicyear.dto.AcademicYearResponse;
import com.attendai.school.academicyear.entity.AcademicYearStatus;
import com.attendai.school.academicyear.service.AcademicYearService;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.entity.SectionStatus;
import com.attendai.school.section.service.SchoolSectionService;
import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import com.attendai.school.subject.repository.ClassSubjectRepository;
import com.attendai.school.subject.service.SchoolSubjectService;
import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.entity.TeacherStatus;
import com.attendai.school.teacher.service.TeacherService;
import com.attendai.school.teacherassignment.dto.ChangeAssignmentStatusRequest;
import com.attendai.school.teacherassignment.dto.CreateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.dto.UpdateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.entity.AssignmentStatus;
import com.attendai.school.teacherassignment.entity.TeacherAssignment;
import com.attendai.school.teacherassignment.exception.TeacherAssignmentNotFoundException;
import com.attendai.school.teacherassignment.mapper.TeacherAssignmentMapper;
import com.attendai.school.teacherassignment.repository.TeacherAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAssignmentServiceImplTest {

    @Mock TeacherAssignmentRepository assignmentRepository;
    @Mock TeacherAssignmentMapper     assignmentMapper;
    @Mock AcademicYearService         academicYearService;
    @Mock SchoolSectionService        sectionService;
    @Mock SchoolSubjectService        subjectService;
    @Mock SchoolClassService          classService;
    @Mock TeacherService              teacherService;
    @Mock ClassSubjectRepository      classSubjectRepository;
    @Mock AuditService                auditService;

    private TeacherAssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherAssignmentServiceImpl(
                assignmentRepository, assignmentMapper, academicYearService,
                sectionService, subjectService, classService, teacherService,
                classSubjectRepository, auditService);
    }

    // -------------------------------------------------------------------------
    // createAssignment — happy path
    // -------------------------------------------------------------------------

    @Test
    void createAssignment_shouldSave_whenAllRulesPass() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        stubSubjectInSchool(30L, 1L);
        when(classSubjectRepository.existsByClassIdAndSubjectId(5L, 30L)).thenReturn(true);
        stubTeacherActive(40L, 1L);
        when(assignmentRepository.existsBySectionIdAndSubjectIdAndAcademicYearId(20L, 30L, 10L))
                .thenReturn(false);

        TeacherAssignment saved = buildAssignment(1L, 1L, 10L, 20L, 30L, 40L, false);
        when(assignmentRepository.save(any())).thenReturn(saved);
        when(assignmentMapper.toResponse(saved)).thenReturn(null);

        service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false));

        verify(assignmentRepository).save(any(TeacherAssignment.class));
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // createAssignment — section validation
    // -------------------------------------------------------------------------

    @Test
    void createAssignment_shouldThrow_whenSectionBelongsToDifferentSchool() {
        stubYearActive(1L, 10L);
        // Section belongs to school 99, not 1
        when(sectionService.findById(20L)).thenReturn(buildSectionResponse(20L, 99L, 5L, 10L));

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void createAssignment_shouldThrow_whenSectionInactive() {
        stubYearActive(1L, 10L);
        when(sectionService.findById(20L)).thenReturn(
                buildSectionResponse(20L, 1L, 5L, 10L, SectionStatus.INACTIVE));

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not ACTIVE");
    }

    // -------------------------------------------------------------------------
    // createAssignment — subject validation
    // -------------------------------------------------------------------------

    @Test
    void createAssignment_shouldThrow_whenSubjectBelongsToDifferentSchool() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        // Subject belongs to school 99
        when(subjectService.findByIdOrThrow(30L)).thenReturn(
                buildSubjectResponse(30L, 99L));

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
    }

    @Test
    void createAssignment_shouldThrow_whenSubjectNotAssociatedWithClass() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        stubSubjectInSchool(30L, 1L);
        when(classSubjectRepository.existsByClassIdAndSubjectId(5L, 30L)).thenReturn(false);

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not associated with class");
    }

    // -------------------------------------------------------------------------
    // createAssignment — teacher validation
    // -------------------------------------------------------------------------

    @Test
    void createAssignment_shouldThrow_whenTeacherNotActive() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        stubSubjectInSchool(30L, 1L);
        when(classSubjectRepository.existsByClassIdAndSubjectId(5L, 30L)).thenReturn(true);
        // Teacher in same school but NOT active
        when(teacherService.findById(1L, 40L)).thenReturn(buildTeacherResponse(40L, 1L));
        when(teacherService.isActive(40L)).thenReturn(false);

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not ACTIVE");
    }

    // -------------------------------------------------------------------------
    // createAssignment — uniqueness rules
    // -------------------------------------------------------------------------

    @Test
    void createAssignment_shouldThrow409_whenDuplicateAssignment() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        stubSubjectInSchool(30L, 1L);
        when(classSubjectRepository.existsByClassIdAndSubjectId(5L, 30L)).thenReturn(true);
        stubTeacherActive(40L, 1L);
        // Duplicate: assignment already exists
        when(assignmentRepository.existsBySectionIdAndSubjectIdAndAcademicYearId(20L, 30L, 10L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, false)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createAssignment_shouldThrow409_whenClassTeacherAlreadyExists() {
        stubYearActive(1L, 10L);
        stubSectionActive(20L, 1L, 5L);
        stubSubjectInSchool(30L, 1L);
        when(classSubjectRepository.existsByClassIdAndSubjectId(5L, 30L)).thenReturn(true);
        stubTeacherActive(40L, 1L);
        when(assignmentRepository.existsBySectionIdAndSubjectIdAndAcademicYearId(20L, 30L, 10L))
                .thenReturn(false);
        // A class teacher already exists
        when(assignmentRepository
                .existsBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(20L, 10L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.createAssignment(1L, 10L, buildCreateRequest(20L, 30L, 40L, true)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("class teacher already exists");
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenAssignmentBelongsToDifferentSchool() {
        TeacherAssignment a = buildAssignment(1L, 2L, 10L, 20L, 30L, 40L, false); // school 2
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(TeacherAssignmentNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(assignmentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(TeacherAssignmentNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldSetInactive() {
        TeacherAssignment a = buildAssignment(1L, 1L, 10L, 20L, 30L, 40L, false);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);
        when(assignmentMapper.toResponse(any())).thenReturn(null);

        ChangeAssignmentStatusRequest req = new ChangeAssignmentStatusRequest();
        req.setStatus(AssignmentStatus.INACTIVE);
        service.changeStatus(1L, 1L, req);

        assertThat(a.getStatus()).isEqualTo(AssignmentStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // deleteAssignment
    // -------------------------------------------------------------------------

    @Test
    void deleteAssignment_shouldSoftDelete_whenNoTimetableEntries() {
        TeacherAssignment a = buildAssignment(1L, 1L, 10L, 20L, 30L, 40L, false);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));
        when(assignmentRepository.save(any())).thenReturn(a);

        service.deleteAssignment(1L, 1L);

        assertThat(a.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // updateAssignment — class teacher conflict
    // -------------------------------------------------------------------------

    @Test
    void updateAssignment_shouldThrow409_whenClassTeacherAlreadyExistsOnDifferentAssignment() {
        TeacherAssignment a = buildAssignment(1L, 1L, 10L, 20L, 30L, 40L, false);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(a));

        // A different assignment (id=99) is already the class teacher
        TeacherAssignment other = buildAssignment(99L, 1L, 10L, 20L, 31L, 41L, true);
        when(assignmentRepository
                .findBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(20L, 10L))
                .thenReturn(Optional.of(other));

        UpdateTeacherAssignmentRequest req = new UpdateTeacherAssignmentRequest();
        req.setClassTeacher(true);

        assertThatThrownBy(() -> service.updateAssignment(1L, 1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenExists() {
        when(assignmentRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    void getClassTeacherForSection_shouldReturnEmpty_whenNone() {
        when(assignmentRepository.findBySectionIdAndAcademicYearIdAndIsClassTeacherTrue(20L, 10L))
                .thenReturn(Optional.empty());
        assertThat(service.getClassTeacherForSection(20L, 10L)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void stubYearActive(Long schoolId, Long yearId) {
        when(academicYearService.findById(schoolId, yearId)).thenReturn(
                AcademicYearResponse.builder().id(yearId).schoolId(schoolId)
                        .name("2025-2026")
                        .startDate(LocalDate.of(2025, 6, 1))
                        .endDate(LocalDate.of(2026, 3, 31))
                        .status(AcademicYearStatus.ACTIVE)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                        .build());
    }

    private void stubSectionActive(Long sectionId, Long schoolId, Long classId) {
        when(sectionService.findById(sectionId)).thenReturn(
                buildSectionResponse(sectionId, schoolId, classId, 10L));
    }

    private void stubSubjectInSchool(Long subjectId, Long schoolId) {
        when(subjectService.findByIdOrThrow(subjectId)).thenReturn(
                buildSubjectResponse(subjectId, schoolId));
    }

    private void stubTeacherActive(Long teacherId, Long schoolId) {
        when(teacherService.findById(schoolId, teacherId))
                .thenReturn(buildTeacherResponse(teacherId, schoolId));
        when(teacherService.isActive(teacherId)).thenReturn(true);
    }

    private SectionResponse buildSectionResponse(Long id, Long schoolId, Long classId,
                                                   Long academicYearId) {
        return buildSectionResponse(id, schoolId, classId, academicYearId, SectionStatus.ACTIVE);
    }

    private SectionResponse buildSectionResponse(Long id, Long schoolId, Long classId,
                                                   Long academicYearId, SectionStatus status) {
        return SectionResponse.builder()
                .id(id).schoolId(schoolId).classId(classId).academicYearId(academicYearId)
                .name("A").status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private SubjectResponse buildSubjectResponse(Long id, Long schoolId) {
        return SubjectResponse.builder()
                .id(id).schoolId(schoolId).name("Mathematics").code("MATH")
                .type(SubjectType.ACADEMIC).status(SubjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TeacherResponse buildTeacherResponse(Long id, Long schoolId) {
        return TeacherResponse.builder()
                .id(id).schoolId(schoolId).personId(99L).status(TeacherStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private TeacherAssignment buildAssignment(Long id, Long schoolId, Long yearId,
                                               Long sectionId, Long subjectId, Long teacherId,
                                               boolean classTeacher) {
        TeacherAssignment a = TeacherAssignment.builder()
                .schoolId(schoolId).academicYearId(yearId)
                .sectionId(sectionId).subjectId(subjectId).teacherId(teacherId)
                .isClassTeacher(classTeacher).status(AssignmentStatus.ACTIVE).build();
        a.setId(id);
        return a;
    }

    private CreateTeacherAssignmentRequest buildCreateRequest(Long sectionId, Long subjectId,
                                                               Long teacherId, boolean classTeacher) {
        CreateTeacherAssignmentRequest req = new CreateTeacherAssignmentRequest();
        req.setSectionId(sectionId);
        req.setSubjectId(subjectId);
        req.setTeacherId(teacherId);
        req.setClassTeacher(classTeacher);
        return req;
    }
}
