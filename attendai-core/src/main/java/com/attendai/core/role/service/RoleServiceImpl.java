package com.attendai.core.role.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.common.security.SecurityContextUtils;
import com.attendai.core.role.dto.AssignRoleRequest;
import com.attendai.core.role.dto.CreateRoleRequest;
import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.dto.RoleSummary;
import com.attendai.core.role.dto.RoleSummaryResponse;
import com.attendai.core.role.dto.UpdateRoleRequest;
import com.attendai.core.role.entity.Role;
import com.attendai.core.role.entity.UserRole;
import com.attendai.core.role.exception.RoleAlreadyExistsException;
import com.attendai.core.role.exception.RoleNotFoundException;
import com.attendai.core.role.mapper.RoleMapper;
import com.attendai.core.role.repository.RoleRepository;
import com.attendai.core.role.repository.UserRoleRepository;
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
public class RoleServiceImpl implements RoleService {

    private final RoleRepository     roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper         roleMapper;
    private final AuditService       auditService;

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new RoleAlreadyExistsException(request.getCode());
        }
        Role role = Role.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Role saved = roleRepository.save(role);
        log.info("Role created | code={}", saved.getCode());
        auditService.log(AuditEventRequest.builder()
                .actionCode("ROLE_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Role")
                .resourceId(String.valueOf(saved.getId()))
                .build());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        return roleMapper.toResponse(requireRole(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleResponse> findByCode(String code) {
        return roleRepository.findByCode(code).map(roleMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleSummaryResponse> listRoles(String search, Pageable pageable) {
        return roleRepository.findBySearch(search, pageable).map(roleMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = requireRole(id);
        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        Role saved = roleRepository.save(role);
        auditService.log(AuditEventRequest.builder()
                .actionCode("ROLE_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Role")
                .resourceId(String.valueOf(id))
                .build());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = requireRole(id);
        if (role.isSystem()) {
            throw new ValidationException("System roles cannot be deleted");
        }
        if (userRoleRepository.existsByRoleId(id)) {
            throw new ValidationException("Role has active user assignments and cannot be deleted");
        }
        role.softDelete();
        roleRepository.save(role);
        auditService.log(AuditEventRequest.builder()
                .actionCode("ROLE_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("Role")
                .resourceId(String.valueOf(id))
                .build());
    }

    @Override
    @Transactional
    public void assignRoleToUser(Long userId, AssignRoleRequest request) {
        if (!roleRepository.existsById(request.getRoleId())) {
            throw new RoleNotFoundException(request.getRoleId());
        }
        if (userRoleRepository.existsByUserIdAndRoleId(userId, request.getRoleId())) {
            throw new ResourceAlreadyExistsException(
                    "User " + userId + " already has role " + request.getRoleId());
        }
        Long actorId = SecurityContextUtils.getCurrentUserId().orElse(null);
        userRoleRepository.save(UserRole.builder()
                .userId(userId)
                .roleId(request.getRoleId())
                .assignedBy(actorId)
                .build());
        auditService.log(AuditEventRequest.builder()
                .actionCode("ROLE_ASSIGNED_TO_USER")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("UserRole")
                .details("{\"userId\":" + userId + ",\"roleId\":" + request.getRoleId() + "}")
                .build());
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        auditService.log(AuditEventRequest.builder()
                .actionCode("ROLE_REMOVED_FROM_USER")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("UserRole")
                .details("{\"userId\":" + userId + ",\"roleId\":" + roleId + "}")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummaryResponse> getRolesForUser(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(opt -> roleMapper.toSummaryResponse(opt.get()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleSummary> findRolesByUserId(Long userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(opt -> new RoleSummary(opt.get().getId(), opt.get().getCode()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return roleRepository.existsById(id);
    }

    private Role requireRole(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
    }
}
