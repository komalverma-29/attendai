package com.attendai.school.subject.service;

import com.attendai.school.subject.dto.AssignSubjectToClassRequest;
import com.attendai.school.subject.dto.ChangeSubjectStatusRequest;
import com.attendai.school.subject.dto.CreateSubjectRequest;
import com.attendai.school.subject.dto.SubjectResponse;
import com.attendai.school.subject.dto.SubjectSummaryResponse;
import com.attendai.school.subject.dto.UpdateSubjectRequest;
import com.attendai.school.subject.entity.SubjectStatus;
import com.attendai.school.subject.entity.SubjectType;

import java.util.List;

public interface SchoolSubjectService {

    SubjectResponse createSubject(Long schoolId, CreateSubjectRequest request);

    SubjectResponse findById(Long schoolId, Long id);

    List<SubjectSummaryResponse> listSubjects(Long schoolId, SubjectType type,
                                               SubjectStatus status, Long classId);

    SubjectResponse updateSubject(Long schoolId, Long id, UpdateSubjectRequest request);

    SubjectResponse changeStatus(Long schoolId, Long id, ChangeSubjectStatusRequest request);

    void assignToClass(Long schoolId, Long subjectId, AssignSubjectToClassRequest request);

    void removeFromClass(Long schoolId, Long subjectId, Long classId);

    void deleteSubject(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-teacher-assignment and school-timetable
    // -------------------------------------------------------------------------

    SubjectResponse findByIdOrThrow(Long subjectId);

    boolean existsById(Long subjectId);

    List<SubjectSummaryResponse> getSubjectsByClassId(Long classId);
}
