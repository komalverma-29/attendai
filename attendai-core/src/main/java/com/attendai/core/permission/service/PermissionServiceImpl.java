package com.attendai.core.permission.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.permission.dto.AssignPermissionRequest;
import com.attendai.core.permission.dto.CreatePermissionRequest;
import com.attendai.core.permission.dto.PermissionResponse;
import com.attendai.core.permission.dto.PermissionSummaryResponse;
import com.attendai.core.permission.dto.UpdatePermissionRequest;
import com.attendai.core.permission.entity.Permission;
import com.attendai.core.permission.entity.RolePermission;
import com.attendai.core.permission.exception.PermissionAlreadyExistsException;
import com.attendai.core.permission.exception.PermissionNotFoundException;
import com.attendai.core.permission.mapper.PermissionMapper;
import com.attendai.core.permission.repository.PermissionRepository;
import com.attendai.core.permission.repository.RolePermissionRepository;
import com.attendai.core.role.exception.RoleNotFoundException;
import com.attendai.core.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository     permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository           roleRepository;
    private final PermissionMapper         permissionMapper;
    private final AuditService             auditService;

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByCode(request.getCode())) {
            throw new PermissionAlreadyExistsException(request.getCode());
        }
        Permission perm = Permission.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .module(request.getModule().toUpperCase())
                .description(request.getDescription())
                .build();
        Permission saved = permissionRepository.save(perm);
        auditService.log(AuditEventRequest.builder()
                .actionCode("PERMISSION_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Permission")
                .resourceId(String.valueOf(saved.getId()))
                .build());
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse findById(Long id) {
        return permissionMapper.toResponse(requirePermission(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PermissionResponse> findByCode(String code) {
        return permissionRepository.findByCode(code).map(permissionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionSummaryResponse> listPermissions(String module, String search, Pageable pageable) {
        return permissionRepository.findByFilters(module, search, pageable)
                .map(permissionMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        Permission perm = requirePermission(id);
        if (request.getName() != null) perm.setName(request.getName());
        if (request.getDescription() != null) perm.setDescription(request.getDescription());
        Permission saved = permissionRepository.save(perm);
        auditService.log(AuditEventRequest.builder()
                .actionCode("PERMISSION_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Permission")
                .resourceId(String.valueOf(id))
                .build());
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission perm = requirePermission(id);
        if (perm.isSystem()) {
            throw new ValidationException("System permissions cannot be deleted");
        }
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ValidationException("Permission has active role assignments and cannot be deleted");
        }
        perm.softDelete();
        permissionRepository.save(perm);
        auditService.log(AuditEventRequest.builder()
                .actionCode("PERMISSION_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Permission")
                .resourceId(String.valueOf(id))
                .build());
    }

    @Override
    @Transactional
    public void assignPermissionToRole(Long roleId, AssignPermissionRequest request) {
        if (!roleRepository.existsById(roleId)) {
            throw new RoleNotFoundException(roleId);
        }
        if (!permissionRepository.existsById(request.getPermissionId())) {
            throw new PermissionNotFoundException(request.getPermissionId());
        }
        if (rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, request.getPermissionId())) {
            throw new ResourceAlreadyExistsException(
                    "Role " + roleId + " already has permission " + request.getPermissionId());
        }
        Long actorId = SecurityContextUtils.getCurrentUserId().orElse(null);
        rolePermissionRepository.save(RolePermission.builder()
                .roleId(roleId)
                .permissionId(request.getPermissionId())
                .assignedBy(actorId)
                .build());
        auditService.log(AuditEventRequest.builder()
                .actionCode("PERMISSION_ASSIGNED_TO_ROLE")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("RolePermission")
                .details("{\"roleId\":" + roleId + ",\"permissionId\":" + request.getPermissionId() + "}")
                .build());
    }

    @Override
    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        auditService.log(AuditEventRequest.builder()
                .actionCode("PERMISSION_REMOVED_FROM_ROLE")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("RolePermission")
                .details("{\"roleId\":" + roleId + ",\"permissionId\":" + permissionId + "}")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermissionSummaryResponse> getPermissionsForRole(Long roleId) {
        return rolePermissionRepository.findByRoleId(roleId).stream()
                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                .filter(Optional::isPresent)
                .map(opt -> permissionMapper.toSummaryResponse(opt.get()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPermissionCodesByUserId(Long userId) {
        return rolePermissionRepository.findPermissionCodesByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findPermissionCodesByRoleId(Long roleId) {
        return rolePermissionRepository.findPermissionCodesByRoleId(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return permissionRepository.existsById(id);
    }

    private Permission requirePermission(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new PermissionNotFoundException(id));
    }
}
