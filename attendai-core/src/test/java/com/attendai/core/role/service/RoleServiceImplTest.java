package com.attendai.core.role.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.role.dto.AssignRoleRequest;
import com.attendai.core.role.dto.CreateRoleRequest;
import com.attendai.core.role.entity.Role;
import com.attendai.core.role.entity.UserRole;
import com.attendai.core.role.exception.RoleAlreadyExistsException;
import com.attendai.core.role.exception.RoleNotFoundException;
import com.attendai.core.role.mapper.RoleMapper;
import com.attendai.core.role.repository.RoleRepository;
import com.attendai.core.role.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock RoleRepository     roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock RoleMapper         roleMapper;
    @Mock AuditService       auditService;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository, userRoleRepository, roleMapper, auditService);
    }

    @Test
    void createRole_shouldSave_whenCodeIsUnique() {
        when(roleRepository.existsByCode("NEW_ROLE")).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(buildRole(1L, "NEW_ROLE", false));

        CreateRoleRequest req = new CreateRoleRequest();
        req.setCode("NEW_ROLE");
        req.setName("New Role");
        roleService.createRole(req);

        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_shouldThrow409_whenCodeDuplicate() {
        when(roleRepository.existsByCode("DUPLICATE")).thenReturn(true);

        CreateRoleRequest req = new CreateRoleRequest();
        req.setCode("DUPLICATE");
        req.setName("Dup");

        assertThatThrownBy(() -> roleService.createRole(req))
                .isInstanceOf(RoleAlreadyExistsException.class);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void deleteRole_shouldThrow400_whenSystemRole() {
        Role role = buildRole(1L, "SYSTEM_ADMIN", true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> roleService.deleteRole(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("System roles cannot be deleted");
    }

    @Test
    void deleteRole_shouldThrow400_whenRoleHasAssignments() {
        Role role = buildRole(1L, "SOME_ROLE", false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByRoleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> roleService.deleteRole(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("active user assignments");
    }

    @Test
    void deleteRole_shouldSoftDelete_whenNoAssignments() {
        Role role = buildRole(1L, "SOME_ROLE", false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByRoleId(1L)).thenReturn(false);
        when(roleRepository.save(any())).thenReturn(role);

        roleService.deleteRole(1L);

        verify(roleRepository).save(role);
    }

    @Test
    void assignRoleToUser_shouldThrow409_whenAlreadyAssigned() {
        when(roleRepository.existsById(2L)).thenReturn(true);
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 2L)).thenReturn(true);

        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleId(2L);

        assertThatThrownBy(() -> roleService.assignRoleToUser(1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void assignRoleToUser_shouldSave_whenNotAlreadyAssigned() {
        when(roleRepository.existsById(2L)).thenReturn(true);
        when(userRoleRepository.existsByUserIdAndRoleId(1L, 2L)).thenReturn(false);
        when(userRoleRepository.save(any())).thenReturn(new UserRole());

        AssignRoleRequest req = new AssignRoleRequest();
        req.setRoleId(2L);
        roleService.assignRoleToUser(1L, req);

        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.findById(99L))
                .isInstanceOf(RoleNotFoundException.class);
    }

    private Role buildRole(Long id, String code, boolean isSystem) {
        return Role.builder().code(code).name(code).isSystem(isSystem).build();
    }
}
