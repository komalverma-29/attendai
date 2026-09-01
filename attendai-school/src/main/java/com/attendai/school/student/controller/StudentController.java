package com.attendai.school.student.controller;

import com.attendai.core.common.pagination.PageRequestParams;
import com.attendai.core.common.response.ApiResponse;
import com.attendai.core.common.response.PageResponse;
import com.attendai.school.student.dto.AssignUserToStudentRequest;
import com.attendai.school.student.dto.ChangeStudentStatusRequest;
import com.attendai.school.student.dto.EnrollStudentRequest;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.dto.StudentSummaryResponse;
import com.attendai.school.student.dto.UpdateStudentRequest;
import com.attendai.school.student.service.StudentService;
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
@RequestMapping("/api/v1/school/schools/{schoolId}/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_CREATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> enrollStudent(
            @PathVariable("schoolId") Long schoolId,
            @Valid @RequestBody EnrollStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(studentService.enrollStudent(schoolId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_READ')")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.findById(schoolId, id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_READ')")
    public ResponseEntity<PageResponse<StudentSummaryResponse>> listStudents(
            @PathVariable("schoolId")                     Long   schoolId,
            @RequestParam(name = "search", required = false) String search,
            @Valid PageRequestParams pageParams) {
        return ResponseEntity.ok(PageResponse.of(
                studentService.listStudents(schoolId, search, pageParams.toPageable())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.updateStudent(schoolId, id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> changeStatus(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody ChangeStudentStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.changeStatus(schoolId, id, request)));
    }

    @PostMapping("/{id}/assign-user")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> assignUser(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id,
            @Valid @RequestBody AssignUserToStudentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                studentService.assignUser(schoolId, id, request)));
    }

    @DeleteMapping("/{id}/remove-user")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_UPDATE')")
    public ResponseEntity<ApiResponse<StudentResponse>> removeUser(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        return ResponseEntity.ok(ApiResponse.success(studentService.removeUser(schoolId, id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHOOL_STUDENT_DELETE')")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable("schoolId") Long schoolId,
            @PathVariable("id")       Long id) {
        studentService.deleteStudent(schoolId, id);
        return ResponseEntity.noContent().build();
    }
}
