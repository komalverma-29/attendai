package com.attendai.school.teacherassignment.service;

import com.attendai.school.teacherassignment.dto.ChangeAssignmentStatusRequest;
import com.attendai.school.teacherassignment.dto.CreateTeacherAssignmentRequest;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentResponse;
import com.attendai.school.teacherassignment.dto.TeacherAssignmentSummaryResponse;
import com.attendai.school.teacherassignment.dto.UpdateTeacherAssignmentRequest;

import java.util.List;
import java.util.Optional;

public interface TeacherAssignmentService {

    TeacherAssignmentResponse createAssignment(Long schoolId, Long academicYearId,
                                                CreateTeacherAssignmentRequest request);

    TeacherAssignmentResponse findById(Long schoolId, Long id);

    List<TeacherAssignmentSummaryResponse> listAssignments(Long schoolId, Long academicYearId,
                                                            Long sectionId, Long teacherId,
                                                            Long subjectId);

    TeacherAssignmentResponse updateAssignment(Long schoolId, Long id,
                                                UpdateTeacherAssignmentRequest request);

    TeacherAssignmentResponse changeStatus(Long schoolId, Long id,
                                            ChangeAssignmentStatusRequest request);

    void deleteAssignment(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-timetable and school-daily-attendance
    // -------------------------------------------------------------------------

    List<TeacherAssignmentResponse> getAssignmentsForSection(Long sectionId,
                                                              Long academicYearId);

    Optional<TeacherAssignmentResponse> getClassTeacherForSection(Long sectionId,
                                                                   Long academicYearId);

    boolean existsById(Long id);
}
