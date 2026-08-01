package com.attendai.core.role.service;

import com.attendai.core.role.dto.AssignRoleRequest;
import com.attendai.core.role.dto.CreateRoleRequest;
import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.dto.RoleSummaryResponse;
import com.attendai.core.role.dto.UpdateRoleRequest;
import com.attendai.core.role.dto.RoleSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoleService {

    RoleResponse createRole(CreateRoleRequest request);
    RoleResponse findById(Long id);
    Optional<RoleResponse> findByCode(String code);
    Page<RoleSummaryResponse> listRoles(String search, Pageable pageable);
    RoleResponse updateRole(Long id, UpdateRoleRequest request);
    void deleteRole(Long id);

    void assignRoleToUser(Long userId, AssignRoleRequest request);
    void removeRoleFromUser(Long userId, Long roleId);
    List<RoleSummaryResponse> getRolesForUser(Long userId);

    /** Used by core-auth to build JWT claims. */
    List<RoleSummary> findRolesByUserId(Long userId);

    boolean existsById(Long id);
}
