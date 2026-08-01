package com.attendai.core.permission.service;

import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.permission.dto.AssignPermissionRequest;
import com.attendai.core.permission.dto.CreatePermissionRequest;
import com.attendai.core.permission.entity.Permission;
import com.attendai.core.permission.entity.RolePermission;
import com.attendai.core.permission.exception.PermissionAlreadyExistsException;
import com.attendai.core.permission.exception.PermissionNotFoundException;
import com.attendai.core.permission.mapper.PermissionMapper;
import com.attendai.core.permission.repository.PermissionRepository;
import com.attendai.core.permission.repository.RolePermissionRepository;
import com.attendai.core.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {

    @Mock PermissionRepository     permissionRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock RoleRepository           roleRepository;
    @Mock PermissionMapper         permissionMapper;
    @Mock AuditService             auditService;

    private PermissionServiceImpl permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionServiceImpl(
                permissionRepository, rolePermissionRepository, roleRepository,
                permissionMapper, auditService);
    }

    @Test
    void createPermission_shouldSave_whenCodeIsUnique() {
        when(permissionRepository.existsByCode("NEW_PERM")).thenReturn(false);
        when(permissionRepository.save(any())).thenReturn(buildPermission(1L, "NEW_PERM", false));

        CreatePermissionRequest req = new CreatePermissionRequest();
        req.setCode("NEW_PERM");
        req.setName("New Perm");
        req.setModule("CORE");
        permissionService.createPermission(req);

        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void createPermission_shouldThrow409_whenCodeDuplicate() {
        when(permissionRepository.existsByCode("DUP")).thenReturn(true);

        CreatePermissionRequest req = new CreatePermissionRequest();
        req.setCode("DUP");
        req.setName("Dup");
        req.setModule("CORE");

        assertThatThrownBy(() -> permissionService.createPermission(req))
                .isInstanceOf(PermissionAlreadyExistsException.class);
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void deletePermission_shouldThrow400_whenSystemPermission() {
        Permission perm = buildPermission(1L, "CORE_USER_READ", true);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(perm));

        assertThatThrownBy(() -> permissionService.deletePermission(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("System permissions cannot be deleted");
    }

    @Test
    void deletePermission_shouldThrow400_whenRoleAssignmentsExist() {
        Permission perm = buildPermission(1L, "CUSTOM_PERM", false);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(perm));
        when(rolePermissionRepository.existsByPermissionId(1L)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.deletePermission(1L))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void assignPermissionToRole_shouldThrow409_whenAlreadyAssigned() {
        when(roleRepository.existsById(1L)).thenReturn(true);
        when(permissionRepository.existsById(2L)).thenReturn(true);
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L)).thenReturn(true);

        AssignPermissionRequest req = new AssignPermissionRequest();
        req.setPermissionId(2L);

        assertThatThrownBy(() -> permissionService.assignPermissionToRole(1L, req))
                .isInstanceOf(ResourceAlreadyExistsException.class);
    }

    @Test
    void assignPermissionToRole_shouldSave_whenNotAlreadyAssigned() {
        when(roleRepository.existsById(1L)).thenReturn(true);
        when(permissionRepository.existsById(2L)).thenReturn(true);
        when(rolePermissionRepository.existsByRoleIdAndPermissionId(1L, 2L)).thenReturn(false);
        when(rolePermissionRepository.save(any())).thenReturn(new RolePermission());

        AssignPermissionRequest req = new AssignPermissionRequest();
        req.setPermissionId(2L);
        permissionService.assignPermissionToRole(1L, req);

        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void findPermissionCodesByUserId_shouldDelegateToRepository() {
        when(rolePermissionRepository.findPermissionCodesByUserId(5L))
                .thenReturn(List.of("CORE_USER_READ", "SCHOOL_STUDENT_CREATE"));

        List<String> codes = permissionService.findPermissionCodesByUserId(5L);

        assertThat(codes).containsExactlyInAnyOrder("CORE_USER_READ", "SCHOOL_STUDENT_CREATE");
    }

    @Test
    void findById_shouldThrow404_whenNotFound() {
        when(permissionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.findById(99L))
                .isInstanceOf(PermissionNotFoundException.class);
    }

    private Permission buildPermission(Long id, String code, boolean isSystem) {
        return Permission.builder().code(code).name(code).module("CORE").isSystem(isSystem).build();
    }
}
