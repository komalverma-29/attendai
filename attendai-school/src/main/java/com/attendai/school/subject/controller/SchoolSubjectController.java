package com.attendai.school.subject.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.subject.dto.AssignSubjectToClassRequest;
import com.attendai.school.subject.dto.ChangeSubjectStatusRequest;
import com.attendai.school.subject.dto.CreateSubjectRequest;
import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.dto.SubjectSummaryResponse;
import com.attendai.school.subject.dto.UpdateSubjectRequest;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;
import com.attendai.school.subject.service.SchoolSubjectService;
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
@RequestMapping("/api/v1/school/schools/{schoolId}/subjects")
@RequiredArgsConstructor
public class SchoolSubjectController {

    private final SchoolSubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_CREATE')")
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(subjectService.createSubject(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_READ')")
    public ResponseEntity<ApiResponse<SubjectResponse>> getSubject(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(subjectService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_READ')")
    public ResponseEntity<ApiResponse<List<SubjectSummaryResponse>>> listSubjects(
            @PathVariable("schoolId")                      Long          schoolId,
            @RequestParam(name = "type",    required = false) SubjectType   type,
            @RequestParam(name = "status",  required = false) SubjectStatus status,
            @RequestParam(name = "classId", required = false) Long          classId) {
        return ResponseEntity.ok(ApiResponse.success(
                subjectService.listSubjects(schoolId, type, status, classId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_UPDATE')")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateSubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                subjectService.updateSubject(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_UPDATE')")
    public ResponseEntity<ApiResponse<SubjectResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeSubjectStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                subjectService.changeStatus(schoolId, id, request)));
    }

    @PostMapping("/{id}/classes")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> assignToClass(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long subjectId,
            @Valid @RequestBody AssignSubjectToClassRequest request) {
        subjectService.assignToClass(schoolId, subjectId, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{id}/classes/{classId}")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_UPDATE')")
    public ResponseEntity<Void> removeFromClass(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long subjectId,
            @PathVariable("classId")  Long classId) {
        subjectService.removeFromClass(schoolId, subjectId, classId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_SUBJECT_DELETE')")
    public ResponseEntity<Void> deleteSubject(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        subjectService.deleteSubject(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
