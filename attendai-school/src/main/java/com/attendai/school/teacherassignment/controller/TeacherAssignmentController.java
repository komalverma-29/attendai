package com.attendai.school.teacherassignment.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.teacherassignment.dto.ChangeAssignmentStatusRequest;
import com.attendai.school.teacherassignment.dto.CreateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentResponse;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentSummaryResponse;
import com.attendai.school.teacherassignment.dto.UpdateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.service.TeacherAssignmentService;
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
@RequestMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/assignments")
@RequiredArgsConstructor
public class TeacherAssignmentController {

    private final TeacherAssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_CREATE')")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> createAssignment(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @Valid @RequestBody CreateTeacherAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        assignmentService.createAssignment(schoolId, academicYearId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<List<TeacherAssignmentSummaryResponse>>> listAssignments(
            @PathVariable("schoolId")                        Long schoolId,
            @PathVariable("academicYearId")                  Long academicYearId,
            @RequestParam(name = "sectionId", required = false) Long sectionId,
            @RequestParam(name = "teacherId", required = false) Long teacherId,
            @RequestParam(name = "subjectId", required = false) Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.listAssignments(schoolId, academicYearId,
                        sectionId, teacherId, subjectId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_READ')")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> getAssignment(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.findById(schoolId, id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> updateAssignment(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateTeacherAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.updateAssignment(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeAssignmentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.changeStatus(schoolId, id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_ASSIGNMENT_DELETE')")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        assignmentService.deleteAssignment(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
