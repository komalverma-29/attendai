package com.attendai.school.school.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.school.dto.ChangeSchoolStatusRequest;
import com.attendai.school.school.dto.CreateSchoolRequest;
import com.attendai.school.school.dto.SchoolResponse;
import com.attendai.school.school.dto.SchoolSummaryResponse;
import com.attendai.school.school.dto.UpdateSchoolRequest;
import com.attendai.school.school.entity.SchoolStatus;
import com.attendai.school.school.entity.SchoolType;
import com.attendai.school.school.service.SchoolService;
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

/**
 * REST controller for school management.
 * Base path: /api/v1/school/schools
 */
@RestController
@RequestMapping("/api/v1/school/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_CREATE')")
    public ResponseEntity<ApiResponse<SchoolResponse>> createSchool(
            @Valid @RequestBody CreateSchoolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(schoolService.createSchool(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_READ')")
    public ResponseEntity<ApiResponse<SchoolResponse>> getSchool(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.success(schoolService.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_READ')")
    public ResponseEntity<PageResponse<SchoolSummaryResponse>> listSchools(
            @RequestParam(name = "status", required = false) SchoolStatus status,
            @RequestParam(name = "type",   required = false) SchoolType   type,
            @RequestParam(name = "search", required = false) String       search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                schoolService.listSchools(status, type, search, pageParams.toPageable())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_UPDATE')")
    public ResponseEntity<ApiResponse<SchoolResponse>> updateSchool(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSchoolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(schoolService.updateSchool(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_UPDATE')")
    public ResponseEntity<ApiResponse<SchoolResponse>> changeStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody ChangeSchoolStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(schoolService.changeStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SCHOOL_DELETE')")
    public ResponseEntity<Void> deleteSchool(@PathVariable("id") Long id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.noContent().build();
    }
}
