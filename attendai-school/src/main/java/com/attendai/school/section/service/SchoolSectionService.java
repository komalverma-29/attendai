package com.attendai.school.section.service;

import com.attendai.school.section.dto.ChangeSectionStatusRequest;
import com.attendai.school.section.dto.CreateSectionRequest;
import com.attendai.school.section.dto.EnrollStudentInSectionRequest;
import com.attendai.school.section.dto.SectionEnrollmentResponse;
import com.attendai.school.section.dto.SectionResponse;
import com.attendai.school.section.dto.SectionSummaryResponse;
import com.attendai.school.section.dto.UpdateSectionRequest;

import java.util.List;
import java.util.Optional;

public interface SchoolSectionService {

    // -------------------------------------------------------------------------
    // Section CRUD
    // -------------------------------------------------------------------------

    SectionResponse createSection(Long schoolId, Long academicYearId, Long classId,
                                   CreateSectionRequest request);

    SectionResponse findById(Long schoolId, Long sectionId);

    List<SectionSummaryResponse> listSections(Long schoolId, Long academicYearId, Long classId);

    SectionResponse updateSection(Long schoolId, Long sectionId, UpdateSectionRequest request);

    SectionResponse changeStatus(Long schoolId, Long sectionId,
                                  ChangeSectionStatusRequest request);

    void deleteSection(Long schoolId, Long sectionId);

    // -------------------------------------------------------------------------
    // Student enrollment
    // -------------------------------------------------------------------------

    SectionEnrollmentResponse enrollStudent(Long schoolId, Long sectionId,
                                             EnrollStudentInSectionRequest request);

    void removeStudent(Long schoolId, Long sectionId, Long studentId);

    List<SectionEnrollmentResponse> getStudentsBySection(Long sectionId);

    Optional<SectionEnrollmentResponse> findStudentEnrollment(Long studentId,
                                                               Long academicYearId);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-daily-attendance, school-timetable, etc.
    // -------------------------------------------------------------------------

    SectionResponse findById(Long sectionId);

    boolean isStudentEnrolledInSection(Long studentId, Long sectionId, Long academicYearId);
}
