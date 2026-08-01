package com.attendai.core.permission.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.core.permission.dto.AssignPermissionRequest;
import com.attendai.core.permission.dto.CreatePermissionRequest;
import com.attendai.core.permission.dto.PermissionResponse;
import com.attendai.core.permission.dto.PermissionSummaryResponse;
import com.attendai.core.permission.dto.UpdatePermissionRequest;
import com.attendai.core.permission.service.PermissionService;
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
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping("/api/v1/core/permissions")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_CREATE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(permissionService.createPermission(req)));
    }

    @GetMapping("/api/v1/core/permissions/{id}")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_READ')")
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.findById(id)));
    }

    @GetMapping("/api/v1/core/permissions")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_READ')")
    public ResponseEntity<PageResponse<PermissionSummaryResponse>> listPermissions(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                permissionService.listPermissions(module, search, pageParams.toPageable())));
    }

    @PutMapping("/api/v1/core/permissions/{id}")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_UPDATE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
            @PathVariable Long id, @Valid @RequestBody UpdatePermissionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(permissionService.updatePermission(id, req)));
    }

    @DeleteMapping("/api/v1/core/permissions/{id}")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_DELETE')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/core/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_ASSIGN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> assignPermission(
            @PathVariable Long roleId, @Valid @RequestBody AssignPermissionRequest req) {
        permissionService.assignPermissionToRole(roleId, req);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "Permission assigned to role successfully")));
    }

    @DeleteMapping("/api/v1/core/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_ASSIGN')")
    public ResponseEntity<Void> removePermission(@PathVariable Long roleId,
                                                  @PathVariable Long permissionId) {
        permissionService.removePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/core/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('CORE_PERMISSION_READ')")
    public ResponseEntity<ApiResponse<List<PermissionSummaryResponse>>> getPermissionsForRole(
            @PathVariable Long roleId) {
        return ResponseEntity.ok(ApiResponse.success(
                permissionService.getPermissionsForRole(roleId)));
    }
}
