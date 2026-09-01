package com.attendai.school.section.controller;

import com.attendai.core.common.response.ApiResponse;
import com.attendai.school.section.dto.ChangeSectionStatusRequest;
import com.attendai.school.section.dto.CreateSectionRequest;
import com.attendai.school.section.dto.EnrollStudentInSectionRequest;
import com.attendai.school.section.dto.SectionEnrollmentResponse;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.dto.SectionSummaryResponse;
import com.attendai.school.section.dto.UpdateSectionRequest;
import com.attendai.school.section.service.SchoolSectionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/school/schools/{schoolId}/academic-years/{academicYearId}/classes/{classId}/sections")
@RequiredArgsConstructor
public class SchoolSectionController {

    private final SchoolSectionService sectionService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_CREATE')")
    public ResponseEntity<ApiResponse<SectionResponse>> createSection(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("classId")        Long classId,
            @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        sectionService.createSection(schoolId, academicYearId, classId, request)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_READ')")
    public ResponseEntity<ApiResponse<List<SectionSummaryResponse>>> listSections(
            @PathVariable("schoolId")       Long schoolId,
            @PathVariable("academicYearId") Long academicYearId,
            @PathVariable("classId")        Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                sectionService.listSections(schoolId, academicYearId, classId)));
    }

    @GetMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_READ')")
    public ResponseEntity<ApiResponse<SectionResponse>> getSection(
            @PathVariable("schoolId")   Long schoolId,
            @PathVariable("sectionId")  Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                sectionService.findById(schoolId, sectionId)));
    }

    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_UPDATE')")
    public ResponseEntity<ApiResponse<SectionResponse>> updateSection(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody UpdateSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                sectionService.updateSection(schoolId, sectionId, request)));
    }

    @PatchMapping("/{sectionId}/status")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_UPDATE')")
    public ResponseEntity<ApiResponse<SectionResponse>> changeStatus(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody ChangeSectionStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                sectionService.changeStatus(schoolId, sectionId, request)));
    }

    @PostMapping("/{sectionId}/students")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_MANAGE')")
    public ResponseEntity<ApiResponse<SectionEnrollmentResponse>> enrollStudent(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody EnrollStudentInSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        sectionService.enrollStudent(schoolId, sectionId, request)));
    }

    @GetMapping("/{sectionId}/students")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_READ')")
    public ResponseEntity<ApiResponse<List<SectionEnrollmentResponse>>> listStudents(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                sectionService.getStudentsBySection(sectionId)));
    }

    @DeleteMapping("/{sectionId}/students/{studentId}")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_MANAGE')")
    public ResponseEntity<Void> removeStudent(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId,
            @PathVariable("studentId") Long studentId) {
        sectionService.removeStudent(schoolId, sectionId, studentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAuthority('SCHOOL_SECTION_DELETE')")
    public ResponseEntity<Void> deleteSection(
            @PathVariable("schoolId")  Long schoolId,
            @PathVariable("sectionId") Long sectionId) {
        sectionService.deleteSection(schoolId, sectionId);
        return ResponseEntity.noContent().build();
    }
}
