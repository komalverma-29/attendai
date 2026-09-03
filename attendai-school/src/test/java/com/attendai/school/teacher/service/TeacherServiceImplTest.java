package com.attendai.school.teacher.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.service.UserService;
import com.attendai.school.teacher.dto.ChangeTeacherStatusRequest;
import com.attendai.school.teacher.dto.CreateTeacherRequest;
import com.attendai.school.teacher.entity.SchoolTeacher;
import com.attendai.school.teacher.entity.TeacherStatus;
import com.attendai.school.teacher.exception.TeacherNotFoundException;
import com.attendai.school.teacher.mapper.SchoolTeacherMapper;
import com.attendai.school.teacher.repository.SchoolTeacherRepository;
import com.attendai.school.school.service.SchoolService;
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
class TeacherServiceImplTest {

    @Mock SchoolTeacherRepository teacherRepository;
    @Mock SchoolTeacherMapper     teacherMapper;
    @Mock SchoolService           schoolService;
    @Mock PersonService           personService;
    @Mock UserService             userService;
    @Mock RoleService             roleService;
    @Mock AuditService            auditService;

    private TeacherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TeacherServiceImpl(teacherRepository, teacherMapper, schoolService,
                personService, userService, roleService, auditService);
    }

    @Test
    void createTeacher_shouldSave_whenNoUserLinked() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(teacherRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(false);

        SchoolTeacher saved = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ACTIVE);
        when(teacherRepository.save(any())).thenReturn(saved);
        when(teacherMapper.toResponse(saved)).thenReturn(null);

        service.createTeacher(1L, buildCreateRequest(10L, null));

        verify(teacherRepository).save(any(SchoolTeacher.class));
        verify(auditService).log(any());
    }

    @Test
    void createTeacher_shouldThrow_whenSchoolNotActive() {
        when(schoolService.isActive(1L)).thenReturn(false);
        assertThatThrownBy(() -> service.createTeacher(1L, buildCreateRequest(10L, null)))
                .isInstanceOf(ValidationException.class);
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void createTeacher_shouldThrow409_whenPersonAlreadyTeacher() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(teacherRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createTeacher(1L, buildCreateRequest(10L, null)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void createTeacher_withUser_shouldThrow_whenPersonUserMismatch() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(teacherRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(false);
        when(userService.findByIdForAuth(20L)).thenReturn(Optional.of(
                new UserAuthProjection(20L, "a@b.com", "hash", "ACTIVE", false)));
        when(userService.findById(20L)).thenReturn(buildUserResponse(20L, 99L)); // different person

        assertThatThrownBy(() -> service.createTeacher(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void findById_shouldThrow404_whenTeacherBelongsToDifferentSchool() {
        SchoolTeacher t = buildTeacher(1L, 2L, 10L, null, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.findById(1L, 1L))
                .isInstanceOf(TeacherNotFoundException.class);
    }

    @Test
    void changeStatus_shouldUpdateStatus() {
        SchoolTeacher t = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.save(any())).thenReturn(t);
        when(teacherMapper.toResponse(any())).thenReturn(null);

        ChangeTeacherStatusRequest req = new ChangeTeacherStatusRequest();
        req.setStatus(TeacherStatus.ON_LEAVE);
        service.changeStatus(1L, 1L, req);

        assertThat(t.getStatus()).isEqualTo(TeacherStatus.ON_LEAVE);
    }

    @Test
    void deleteTeacher_shouldSoftDelete() {
        SchoolTeacher t = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teacherRepository.save(any())).thenReturn(t);

        service.deleteTeacher(1L, 1L);

        assertThat(t.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    @Test
    void removeUser_shouldThrow_whenNoUserLinked() {
        SchoolTeacher t = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ACTIVE); // userId=null
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.removeUser(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("does not have a linked user");
    }

    // -------------------------------------------------------------------------
    // Internal APIs — isActive, existsById
    // -------------------------------------------------------------------------

    @Test
    void isActive_shouldReturnTrue_whenTeacherStatusIsActive() {
        SchoolTeacher t = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThat(service.isActive(1L)).isTrue();
    }

    @Test
    void isActive_shouldReturnFalse_whenTeacherStatusIsOnLeave() {
        SchoolTeacher t = buildTeacher(1L, 1L, 10L, null, TeacherStatus.ON_LEAVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(t));
        assertThat(service.isActive(1L)).isFalse();
    }

    @Test
    void isActive_shouldReturnFalse_whenTeacherNotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.isActive(99L)).isFalse();
    }

    @Test
    void existsById_shouldReturnTrue_whenTeacherExists() {
        when(teacherRepository.existsById(1L)).thenReturn(true);
        assertThat(service.existsById(1L)).isTrue();
    }

    // Helpers

    private SchoolTeacher buildTeacher(Long id, Long schoolId, Long personId,
                                        Long userId, TeacherStatus status) {
        SchoolTeacher t = SchoolTeacher.builder()
                .schoolId(schoolId).personId(personId).userId(userId).status(status).build();
        t.setId(id);
        return t;
    }

    private CreateTeacherRequest buildCreateRequest(Long personId, Long userId) {
        CreateTeacherRequest req = new CreateTeacherRequest();
        req.setPersonId(personId);
        req.setUserId(userId);
        return req;
    }

    private UserResponse buildUserResponse(Long userId, Long personId) {
        return UserResponse.builder().id(userId).personId(personId).email("a@b.com")
                .username("ab").status(UserStatus.ACTIVE).mustChangePassword(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
