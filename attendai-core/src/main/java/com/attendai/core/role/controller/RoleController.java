package com.attendai.core.role.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.role.dto.AssignRoleRequest;
import com.attendai.core.role.dto.CreateRoleRequest;
import com.attendai.core.role.dto.RoleResponse;
import com.attendai.core.role.dto.RoleSummaryResponse;
import com.attendai.core.role.dto.UpdateRoleRequest;
import com.attendai.core.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    // -------------------------------------------------------------------------
    // Role CRUD
    // -------------------------------------------------------------------------

    @PostMapping("/api/v1/core/roles")
    @PreAuthorize("hasAuthority('CORE_ROLE_CREATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(@Valid @RequestBody CreateRoleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(roleService.createRole(req)));
    }

    @GetMapping("/api/v1/core/roles/{id}")
    @PreAuthorize("hasAuthority('CORE_ROLE_READ')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(roleService.findById(id)));
    }

    @GetMapping("/api/v1/core/roles")
    @PreAuthorize("hasAuthority('CORE_ROLE_READ')")
    public ResponseEntity<PageResponse<RoleSummaryResponse>> listRoles(
            @RequestParam(required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(roleService.listRoles(search, pageParams.toPageable())));
    }

    @PutMapping("/api/v1/core/roles/{id}")
    @PreAuthorize("hasAuthority('CORE_ROLE_UPDATE')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateRoleRequest req) {
        return ResponseEntity.ok(ApiResponse.success(roleService.updateRole(id, req)));
    }

    @DeleteMapping("/api/v1/core/roles/{id}")
    @PreAuthorize("hasAuthority('CORE_ROLE_DELETE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // User-role assignment
    // -------------------------------------------------------------------------

    @PostMapping("/api/v1/core/users/{userId}/roles")
    @PreAuthorize("hasAuthority('CORE_ROLE_ASSIGN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> assignRole(
            @PathVariable Long userId, @Valid @RequestBody AssignRoleRequest req) {
        roleService.assignRoleToUser(userId, req);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Role assigned successfully")));
    }

    @DeleteMapping("/api/v1/core/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('CORE_ROLE_ASSIGN')")
    public ResponseEntity<Void> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/core/users/{userId}/roles")
    @PreAuthorize("hasAuthority('CORE_ROLE_READ')")
    public ResponseEntity<ApiResponse<List<RoleSummaryResponse>>> getRolesForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(roleService.getRolesForUser(userId)));
    }
}
