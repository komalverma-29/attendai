package com.attendai.school.administrator.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.administrator.dto.AdministratorResponse;
import com.attendai.school.administrator.dto.AdministratorSummaryResponse;
import com.attendai.school.administrator.dto.ChangeAdministratorStatusRequest;
import com.attendai.school.administrator.dto.CreateAdministratorRequest;
import com.attendai.school.administrator.dto.UpdateAdministratorRequest;
import com.attendai.school.administrator.entity.AdministratorStatus;
import com.attendai.school.administrator.service.AdministratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/administrators")
@RequiredArgsConstructor
public class AdministratorController {

    private final AdministratorService administratorService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_CREATE')")
    public ResponseEntity<ApiResponse<AdministratorResponse>> createAdministrator(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateAdministratorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        administratorService.createAdministrator(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_READ')")
    public ResponseEntity<ApiResponse<AdministratorResponse>> getAdministrator(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                administratorService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_READ')")
    public ResponseEntity<PageResponse<AdministratorSummaryResponse>> listAdministrators(
            @PathVariable("schoolId")                    Long                schoolId,
            @RequestParam(name = "status", required = false) AdministratorStatus status,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                administratorService.listAdministrators(schoolId, status,
                        pageParams.toPageable())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_UPDATE')")
    public ResponseEntity<ApiResponse<AdministratorResponse>> updateAdministrator(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateAdministratorRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                administratorService.updateAdministrator(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_UPDATE')")
    public ResponseEntity<ApiResponse<AdministratorResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeAdministratorStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                administratorService.changeStatus(schoolId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_ADMINISTRATOR_DELETE')")
    public ResponseEntity<Void> deleteAdministrator(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        administratorService.deleteAdministrator(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
