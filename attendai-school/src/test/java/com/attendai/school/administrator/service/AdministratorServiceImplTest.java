package com.attendai.school.administrator.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.service.UserService;
import com.attendai.school.administrator.dto.ChangeAdministratorStatusRequest;
import com.attendai.school.administrator.dto.CreateAdministratorRequest;
import com.attendai.school.administrator.entity.AdministratorStatus;
import com.attendai.school.administrator.entity.SchoolAdministrator;
import com.attendai.school.administrator.exception.AdministratorNotFoundException;
import com.attendai.school.administrator.mapper.SchoolAdministratorMapper;
import com.attendai.school.administrator.repository.SchoolAdministratorRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministratorServiceImplTest {

    @Mock SchoolAdministratorRepository adminRepository;
    @Mock SchoolAdministratorMapper     adminMapper;
    @Mock SchoolService                 schoolService;
    @Mock PersonService                 personService;
    @Mock UserService                   userService;
    @Mock RoleService                   roleService;
    @Mock AuditService                  auditService;

    private AdministratorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdministratorServiceImpl(adminRepository, adminMapper, schoolService,
                personService, userService, roleService, auditService);
    }

    // -------------------------------------------------------------------------
    // createAdministrator
    // -------------------------------------------------------------------------

    @Test
    void createAdministrator_shouldSave_whenValid() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(userService.findByIdForAuth(20L)).thenReturn(Optional.of(
                new UserAuthProjection(20L, "a@b.com", "hash", "ACTIVE", false)));
        when(userService.findById(20L)).thenReturn(buildUserResponse(20L, 10L));
        when(adminRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(false);
        when(adminRepository.findByUserId(20L)).thenReturn(Optional.empty());
        when(roleService.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        SchoolAdministrator saved = buildAdmin(1L, 1L, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.save(any())).thenReturn(saved);
        when(adminMapper.toResponse(saved)).thenReturn(null);

        service.createAdministrator(1L, buildCreateRequest(10L, 20L));

        verify(adminRepository).save(any(SchoolAdministrator.class));
        verify(auditService).log(any());
    }

    @Test
    void createAdministrator_shouldThrow_whenSchoolNotActive() {
        when(schoolService.isActive(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.createAdministrator(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(ValidationException.class);
        verify(adminRepository, never()).save(any());
    }

    @Test
    void createAdministrator_shouldThrow_whenPersonNotFound() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> service.createAdministrator(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(com.attendai.core.common.exception.ResourceNotFoundException.class);
    }

    @Test
    void createAdministrator_shouldThrow_whenUserNotActive() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(userService.findByIdForAuth(20L)).thenReturn(Optional.of(
                new UserAuthProjection(20L, "a@b.com", "hash", "INACTIVE", false)));

        assertThatThrownBy(() -> service.createAdministrator(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAdministrator_shouldThrow_whenPersonUserMismatch() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(userService.findByIdForAuth(20L)).thenReturn(Optional.of(
                new UserAuthProjection(20L, "a@b.com", "hash", "ACTIVE", false)));
        // User belongs to different person (personId=99, not 10)
        when(userService.findById(20L)).thenReturn(buildUserResponse(20L, 99L));

        assertThatThrownBy(() -> service.createAdministrator(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("same person record");
    }

    @Test
    void createAdministrator_shouldThrow409_whenPersonAlreadyAdminInSchool() {
        when(schoolService.isActive(1L)).thenReturn(true);
        when(personService.existsById(10L)).thenReturn(true);
        when(userService.findByIdForAuth(20L)).thenReturn(Optional.of(
                new UserAuthProjection(20L, "a@b.com", "hash", "ACTIVE", false)));
        when(userService.findById(20L)).thenReturn(buildUserResponse(20L, 10L));
        when(adminRepository.existsByPersonIdAndSchoolId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createAdministrator(1L, buildCreateRequest(10L, 20L)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L, 99L))
                .isInstanceOf(AdministratorNotFoundException.class);
    }

    @Test
    void findById_shouldThrow404_whenAdminBelongsToDifferentSchool() {
        SchoolAdministrator admin = buildAdmin(1L, 2L /* different school */, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.findById(1L /* requested school */, 1L))
                .isInstanceOf(AdministratorNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // changeStatus — last-admin guard
    // -------------------------------------------------------------------------

    @Test
    void changeStatus_shouldThrow_whenDeactivatingLastAdmin() {
        SchoolAdministrator admin = buildAdmin(1L, 1L, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.countBySchoolIdAndStatus(1L, AdministratorStatus.ACTIVE)).thenReturn(1L);

        ChangeAdministratorStatusRequest req = new ChangeAdministratorStatusRequest();
        req.setStatus(AdministratorStatus.INACTIVE);

        assertThatThrownBy(() -> service.changeStatus(1L, 1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("last active administrator");
    }

    @Test
    void changeStatus_shouldDeactivate_whenOtherActiveAdminExists() {
        SchoolAdministrator admin = buildAdmin(1L, 1L, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.countBySchoolIdAndStatus(1L, AdministratorStatus.ACTIVE)).thenReturn(2L);
        when(adminRepository.save(any())).thenReturn(admin);
        when(adminMapper.toResponse(any())).thenReturn(null);
        when(roleService.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        ChangeAdministratorStatusRequest req = new ChangeAdministratorStatusRequest();
        req.setStatus(AdministratorStatus.INACTIVE);
        service.changeStatus(1L, 1L, req);

        assertThat(admin.getStatus()).isEqualTo(AdministratorStatus.INACTIVE);
    }

    // -------------------------------------------------------------------------
    // deleteAdministrator — last-admin guard
    // -------------------------------------------------------------------------

    @Test
    void deleteAdministrator_shouldThrow_whenDeletingLastAdmin() {
        SchoolAdministrator admin = buildAdmin(1L, 1L, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.countBySchoolIdAndStatus(1L, AdministratorStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAdministrator(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("last active administrator");
    }

    @Test
    void deleteAdministrator_shouldSoftDelete_whenOtherActiveAdminExists() {
        SchoolAdministrator admin = buildAdmin(1L, 1L, 10L, 20L, AdministratorStatus.ACTIVE);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.countBySchoolIdAndStatus(1L, AdministratorStatus.ACTIVE)).thenReturn(2L);
        when(adminRepository.save(any())).thenReturn(admin);
        when(roleService.findByCode("SCHOOL_ADMIN")).thenReturn(Optional.empty());

        service.deleteAdministrator(1L, 1L);

        assertThat(admin.isDeleted()).isTrue();
        verify(auditService).log(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolAdministrator buildAdmin(Long id, Long schoolId, Long personId,
                                            Long userId, AdministratorStatus status) {
        SchoolAdministrator a = SchoolAdministrator.builder()
                .schoolId(schoolId).personId(personId).userId(userId).status(status).build();
        a.setId(id);
        return a;
    }

    private CreateAdministratorRequest buildCreateRequest(Long personId, Long userId) {
        CreateAdministratorRequest req = new CreateAdministratorRequest();
        req.setPersonId(personId);
        req.setUserId(userId);
        req.setDesignation("Principal");
        return req;
    }

    private UserResponse buildUserResponse(Long userId, Long personId) {
        return UserResponse.builder()
                .id(userId).personId(personId).email("a@b.com").username("ab")
                .status(UserStatus.ACTIVE).mustChangePassword(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }
}
