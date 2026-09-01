package com.attendai.school.schoolclass.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.schoolclass.dto.ChangeClassStatusRequest;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.dto.ClassSummaryResponse;
import com.attendai.school.schoolclass.dto.CreateClassRequest;
import com.attendai.school.schoolclass.dto.UpdateClassRequest;
import com.attendai.school.schoolclass.entity.ClassStatus;
import com.attendai.school.schoolclass.service.SchoolClassService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_CREATE')")
    public ResponseEntity<ApiResponse<ClassResponse>> createClass(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(schoolClassService.createClass(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_READ')")
    public ResponseEntity<ApiResponse<ClassResponse>> getClass(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(schoolClassService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_READ')")
    public ResponseEntity<ApiResponse<List<ClassSummaryResponse>>> listClasses(
            @PathVariable("schoolId")                     Long        schoolId,
            @RequestParam(name = "status", required = false) ClassStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success(schoolClassService.listClasses(schoolId, status)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_UPDATE')")
    public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolClassService.updateClass(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_UPDATE')")
    public ResponseEntity<ApiResponse<ClassResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeClassStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                schoolClassService.changeStatus(schoolId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_CLASS_DELETE')")
    public ResponseEntity<Void> deleteClass(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        schoolClassService.deleteClass(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
