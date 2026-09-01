package com.attendai.school.subject.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.service.SchoolClassService;
import com.attendai.school.subject.dto.AssignSubjectToClassRequest;
import com.attendai.school.subject.dto.ChangeSubjectStatusRequest;
import com.attendai.school.subject.dto.CreateSubjectRequest;
import com.attendai.school.subject.dto.UpdateSubjectRequest;
import com.attendai.school.subject.entity.ClassSubject;
import com.attendai.school.subject.entity.SchoolSubject;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import com.attendai.school.subject.exception.SubjectNotFoundException;
import com.attendai.school.subject.mapper.SchoolSubjectMapper;
import com.attendai.school.subject.repository.ClassSubjectRepository;
import com.attendai.school.subject.repository.SchoolSubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolSubjectServiceImplTest {

    @Mock SchoolSubjectRepository subjectRepository;
    @Mock ClassSubjectRepository  classSubjectRepository;
    @Mock SchoolSubjectMapper     subjectMapper;
    @Mock SchoolService           schoolService;
    @Mock SchoolClassService      classService;
    @Mock AuditService            auditService;

    private SchoolSubjectServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SchoolSubjectServiceImpl(
                subjectRepository, classSubjectRepository, subjectMapper,
                schoolService, classService, auditService);
    }

    // -------------------------------------------------------------------------
    // createSubject
    // -------------------------------------------------------------------------

    @Test
    void createSubject_shouldSave_whenValidRequest() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(subjectRepository.existsBySchoolIdAndName(1L, "Mathematics")).thenReturn(false);
        when(subjectRepository.existsBySchoolIdAndCode(1L, "MATH")).thenReturn(false);
        SchoolSubject saved = buildSubject(1L, 1L, "Mathematics", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.save(any())).thenReturn(saved);
        when(subjectMapper.toResponse(saved)).thenReturn(null);

        service.createSubject(1L, buildCreateRequest("Mathematics", "MATH", SubjectType.ACADEMIC));

        verify(subjectRepository).save(any(SchoolSubject.class));
        verify(auditService).log(any());
    }

    @Test
    void createSubject_shouldThrow_whenSchoolNotActive() {
        when(schoolService.isActive(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.createSubject(1L,
                buildCreateRequest("Maths", "MATH", SubjectType.ACADEMIC)))
                .isInstanceOf(ValidationException.class);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void createSubject_shouldThrow409_whenNameDuplicate() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(subjectRepository.existsBySchoolIdAndName(1L, "Mathematics")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubject(1L,
                buildCreateRequest("Mathematics", "MATH", SubjectType.ACADEMIC)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createSubject_shouldThrow409_whenCodeDuplicate() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(subjectRepository.existsBySchoolIdAndName(1L, "Mathematics")).thenReturn(false);
        when(subjectRepository.existsBySchoolIdAndCode(1L, "MATH")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubject(1L,
                buildCreateRequest("Mathematics", "MATH", SubjectType.ACADEMIC)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenSubjectBelongsToDifferentSchool() {
        SchoolSubject s = buildSubject(1L, 2L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE); // schoolId=2
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenSubjectNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateSubject
    // -------------------------------------------------------------------------

    @Test
    void updateSubject_shouldUpdate_whenNameChanges() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));
        when(subjectRepository.existsBySchoolIdAndName(1L, "Mathematics")).thenReturn(false);
        when(subjectRepository.save(any())).thenReturn(s);
        when(subjectMapper.toResponse(any())).thenReturn(null);

        UpdateSubjectRequest req = new UpdateSubjectRequest();
        req.setName("Mathematics");
        service.updateSubject(1L, 1L, req);

        assertThat(s.getName()).isEqualTo("Mathematics");
    }

    @Test
    void updateSubject_shouldThrow409_whenNewNameDuplicate() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));
        when(subjectRepository.existsBySchoolIdAndName(1L, "Physics")).thenReturn(true);

        UpdateSubjectRequest req = new UpdateSubjectRequest();
        req.setName("Physics");

        assertThatThrownBy(() -> service.updateSubject(1L, 1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldSetInactive() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));
        when(subjectRepository.save(any())).thenReturn(s);
        when(subjectMapper.toResponse(any())).thenReturn(null);

        ChangeSubjectStatusRequest req = new ChangeSubjectStatusRequest();
        req.setStatus(SubjectStatus.INACTIVE);
        service.changeStatus(1L, 1L, req);

        assertThat(s.getStatus()).isEqualTo(SubjectStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // assignToClass
    // -------------------------------------------------------------------------

    @Test
    void assignToClass_shouldSave_whenValid() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));

        ClassResponse classResponse = buildClassResponse(2L, 1L);
        when(classService.findByIdOrThrow(2L)).thenReturn(classResponse);
        when(classSubjectRepository.existsByClassIdAndSubjectId(2L, 1L)).thenReturn(false);
        when(classSubjectRepository.save(any())).thenReturn(new ClassSubject());

        AssignSubjectToClassRequest req = new AssignSubjectToClassRequest();
        req.setClassId(2L);
        service.assignToClass(1L, 1L, req);

        verify(classSubjectRepository).save(any(ClassSubject.class));
        verify(auditService).log(any());
    }

    @Test
    void assignToClass_shouldThrow_whenClassBelongsToDifferentSchool() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));

        ClassResponse classResponse = buildClassResponse(2L, 99L); // school 99 != 1
        when(classService.findByIdOrThrow(2L)).thenReturn(classResponse);

        AssignSubjectToClassRequest req = new AssignSubjectToClassRequest();
        req.setClassId(2L);

        assertThatThrownBy(() -> service.assignToClass(1L, 1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not belong to school");
    }

    @Test
    void assignToClass_shouldThrow409_whenAlreadyAssigned() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));

        ClassResponse classResponse = buildClassResponse(2L, 1L);
        when(classService.findByIdOrThrow(2L)).thenReturn(classResponse);
        when(classSubjectRepository.existsByClassIdAndSubjectId(2L, 1L)).thenReturn(true);

        AssignSubjectToClassRequest req = new AssignSubjectToClassRequest();
        req.setClassId(2L);

        assertThatThrownBy(() -> service.assignToClass(1L, 1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // removeFromClass
    // -------------------------------------------------------------------------

    @Test
    void removeFromClass_shouldDelete_whenAssigned() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));

        ClassSubject cs = new ClassSubject();
        when(classSubjectRepository.findByClassIdAndSubjectId(2L, 1L))
                .thenReturn(Optional.of(cs));

        service.removeFromClass(1L, 1L, 2L);

        verify(classSubjectRepository).delete(cs);
        verify(auditService).log(any());
    }

    @Test
    void removeFromClass_shouldThrow_whenNotAssigned() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));
        when(classSubjectRepository.findByClassIdAndSubjectId(2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeFromClass(1L, 1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteSubject
    // -------------------------------------------------------------------------

    @Test
    void deleteSubject_shouldSoftDelete_whenNoActiveAssignments() {
        SchoolSubject s = buildSubject(1L, 1L, "Maths", "MATH",
                SubjectType.ACADEMIC, SubjectStatus.ACTIVE);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(s));
        when(subjectRepository.save(any())).thenReturn(s);

        service.deleteSubject(1L, 1L);

        assertThat(s.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenSubjectExists() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    void findByIdOrThrow_shouldThrow404_whenNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByIdOrThrow(99L))
                .isInstanceOf(SubjectNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolSubject buildSubject(Long id, Long schoolId, String name, String code,
                                        SubjectType type, SubjectStatus status) {
        SchoolSubject s = SchoolSubject.builder()
                .schoolId(schoolId).name(name).code(code).type(type).status(status).build();
        s.setId(id);
        return s;
    }

    private ClassResponse buildClassResponse(Long classId, Long schoolId) {
        return ClassResponse.builder()
                .id(classId).schoolId(schoolId).name("Grade 5").gradeOrder(5)
                .status(ClassStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private CreateSubjectRequest buildCreateRequest(String name, String code, SubjectType type) {
        CreateSubjectRequest req = new CreateSubjectRequest();
        req.setName(name);
        req.setCode(code);
        req.setType(type);
        return req;
    }
}
