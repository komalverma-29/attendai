package com.attendai.school.teacher.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.teacher.dto.AssignUserToTeacherRequest;
import com.attendai.school.teacher.dto.ChangeTeacherStatusRequest;
import com.attendai.school.teacher.dto.CreateTeacherRequest;
import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.dto.TeacherSummaryResponse;
import com.attendai.school.teacher.dto.UpdateTeacherRequest;
import com.attendai.school.teacher.service.TeacherService;
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
@RequestMapping("/api/v1/school/schools/{schoolId}/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_CREATE')")
    public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody CreateTeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(teacherService.createTeacher(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_READ')")
    public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_READ')")
    public ResponseEntity<PageResponse<TeacherSummaryResponse>> listTeachers(
            @PathVariable("schoolId")                    Long   schoolId,
            @RequestParam(name = "search", required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                teacherService.listTeachers(schoolId, search, pageParams.toPageable())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherResponse>> updateTeacher(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateTeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                teacherService.updateTeacher(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeTeacherStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                teacherService.changeStatus(schoolId, id, request)));
    }

    @PostMapping("/{id}/assign-user")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherResponse>> assignUser(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody AssignUserToTeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                teacherService.assignUser(schoolId, id, request)));
    }

    @DeleteMapping("/{id}/remove-user")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_UPDATE')")
    public ResponseEntity<ApiResponse<TeacherResponse>> removeUser(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.removeUser(schoolId, id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_TEACHER_DELETE')")
    public ResponseEntity<Void> deleteTeacher(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        teacherService.deleteTeacher(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
