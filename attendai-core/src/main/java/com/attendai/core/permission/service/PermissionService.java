package com.attendai.core.permission.service;

import com.attendai.core.permission.dto.AssignPermissionRequest;
import com.attendai.core.permission.dto.CreatePermissionRequest;
import com.attendai.core.permission.dto.PermissionResponse;
import com.attendai.core.permission.dto.PermissionSummaryResponse;
import com.attendai.core.permission.dto.UpdatePermissionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PermissionService {

    PermissionResponse createPermission(CreatePermissionRequest request);
    PermissionResponse findById(Long id);
    Optional<PermissionResponse> findByCode(String code);
    Page<PermissionSummaryResponse> listPermissions(String module, String search, Pageable pageable);
    PermissionResponse updatePermission(Long id, UpdatePermissionRequest request);
    void deletePermission(Long id);

    void assignPermissionToRole(Long roleId, AssignPermissionRequest request);
    void removePermissionFromRole(Long roleId, Long permissionId);
    List<PermissionSummaryResponse> getPermissionsForRole(Long roleId);

    /** Used by core-auth: resolve all permission codes for a user. Single JOIN query. */
    List<String> findPermissionCodesByUserId(Long userId);

    /** Used by core-auth: resolve permission codes for a single role. */
    List<String> findPermissionCodesByRoleId(Long roleId);

    boolean existsById(Long id);
}
