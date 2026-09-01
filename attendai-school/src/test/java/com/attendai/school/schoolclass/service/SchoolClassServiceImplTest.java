package com.attendai.school.schoolclass.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.schoolclass.dto.ChangeClassStatusRequest;
import com.attendai.school.schoolclass.dto.CreateClassRequest;
import com.attendai.school.schoolclass.dto.UpdateClassRequest;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.entity.SchoolClass;
import com.attendai.school.schoolclass.exception.ClassNotFoundException;
import com.attendai.school.schoolclass.mapper.SchoolClassMapper;
import com.attendai.school.schoolclass.repository.SchoolClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolClassServiceImplTest {

    @Mock SchoolClassRepository classRepository;
    @Mock SchoolClassMapper     classMapper;
    @Mock SchoolService         schoolService;
    @Mock AuditService          auditService;

    private SchoolClassServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SchoolClassServiceImpl(classRepository, classMapper, schoolService, auditService);
    }

    // -------------------------------------------------------------------------
    // createClass
    // -------------------------------------------------------------------------

    @Test
    void createClass_shouldSave_whenValidRequest() {
        // Arrange
        when(schoolService.isActive(1L)).thenReturn(true);
        when(classRepository.existsBySchoolIdAndName(1L, "Grade 5")).thenReturn(false);
        SchoolClass saved = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.save(any())).thenReturn(saved);
        when(classMapper.toResponse(saved)).thenReturn(null);

        // Act
        service.createClass(1L, buildCreateRequest("Grade 5", 5));

        // Assert
        verify(classRepository).save(any(SchoolClass.class));
        verify(auditService).log(any());
    }

    @Test
    void createClass_shouldThrow_whenSchoolNotActive() {
        when(schoolService.isActive(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.createClass(1L, buildCreateRequest("Grade 5", 5)))
                .isInstanceOf(ValidationException.class);
        verify(classRepository, never()).save(any());
    }

    @Test
    void createClass_shouldThrow409_whenNameDuplicate() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(classRepository.existsBySchoolIdAndName(1L, "Grade 5")).thenReturn(true);

        assertThatThrownBy(() -> service.createClass(1L, buildCreateRequest("Grade 5", 5)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenClassBelongsToDifferentSchool() {
        SchoolClass c = buildClass(1L, 2L, "Grade 5", 5, ClassStatus.ACTIVE); // schoolId=2
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenClassNotFound() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateClass
    // -------------------------------------------------------------------------

    @Test
    void updateClass_shouldUpdate_whenNameChanges() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        when(classRepository.existsBySchoolIdAndName(1L, "Grade Five")).thenReturn(false);
        when(classRepository.save(any())).thenReturn(c);
        when(classMapper.toResponse(any())).thenReturn(null);

        UpdateClassRequest req = new UpdateClassRequest();
        req.setName("Grade Five");
        service.updateClass(1L, 1L, req);

        assertThat(c.getName()).isEqualTo("Grade Five");
    }

    @Test
    void updateClass_shouldThrow409_whenNewNameAlreadyExists() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        when(classRepository.existsBySchoolIdAndName(1L, "Grade 6")).thenReturn(true);

        UpdateClassRequest req = new UpdateClassRequest();
        req.setName("Grade 6");

        assertThatThrownBy(() -> service.updateClass(1L, 1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldSetInactive_whenActive() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        when(classRepository.save(any())).thenReturn(c);
        when(classMapper.toResponse(any())).thenReturn(null);

        ChangeClassStatusRequest req = new ChangeClassStatusRequest();
        req.setStatus(ClassStatus.INACTIVE);
        service.changeStatus(1L, 1L, req);

        assertThat(c.getStatus()).isEqualTo(ClassStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // deleteClass
    // -------------------------------------------------------------------------

    @Test
    void deleteClass_shouldSoftDelete_whenNoSections() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        when(classRepository.save(any())).thenReturn(c);

        service.deleteClass(1L, 1L);

        assertThat(c.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Test
    void existsById_shouldReturnTrue_whenClassExists() {
        when(classRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    @Test
    void isActive_shouldReturnTrue_whenStatusActive() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.ACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThat(service.isActive(1L)).isTrue();
    }

    @Test
    void isActive_shouldReturnFalse_whenStatusInactive() {
        SchoolClass c = buildClass(1L, 1L, "Grade 5", 5, ClassStatus.INACTIVE);
        when(classRepository.findById(1L)).thenReturn(Optional.of(c));
        assertThat(service.isActive(1L)).isFalse();
    }

    @Test
    void isActive_shouldReturnFalse_whenClassNotFound() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.isActive(99L)).isFalse();
    }

    @Test
    void findByIdOrThrow_shouldThrow404_whenClassNotFound() {
        when(classRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByIdOrThrow(99L))
                .isInstanceOf(ClassNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolClass buildClass(Long id, Long schoolId, String name,
                                    int gradeOrder, ClassStatus status) {
        SchoolClass c = SchoolClass.builder()
                .schoolId(schoolId).name(name).gradeOrder(gradeOrder).status(status).build();
        c.setId(id);
        return c;
    }

    private CreateClassRequest buildCreateRequest(String name, int gradeOrder) {
        CreateClassRequest req = new CreateClassRequest();
        req.setName(name);
        req.setGradeOrder(gradeOrder);
        return req;
    }
}
