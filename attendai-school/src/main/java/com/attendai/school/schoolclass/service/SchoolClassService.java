package com.attendai.school.schoolclass.service;

import com.attendai.school.schoolclass.dto.ChangeClassStatusRequest;
import com.attendai.school.schoolclass.dto.ClassResponse;
import com.attendai.school.schoolclass.dto.ClassSummaryResponse;
import com.attendai.school.schoolclass.dto.CreateClassRequest;
import com.attendai.school.schoolclass.dto.UpdateClassRequest;
import com.attendai.school.schoolclass.entity.ClassStatus;

import java.util.List;

public interface SchoolClassService {

    ClassResponse createClass(Long schoolId, CreateClassRequest request);

    ClassResponse findById(Long schoolId, Long id);

    List<ClassSummaryResponse> listClasses(Long schoolId, ClassStatus status);

    ClassResponse updateClass(Long schoolId, Long id, UpdateClassRequest request);

    ClassResponse changeStatus(Long schoolId, Long id, ChangeClassStatusRequest request);

    void deleteClass(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-section and school-subject (Round 2+)
    // -------------------------------------------------------------------------

    boolean existsById(Long id);

    ClassResponse findByIdOrThrow(Long id);

    boolean isActive(Long id);
}
