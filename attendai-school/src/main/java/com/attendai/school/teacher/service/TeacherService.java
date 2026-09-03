package com.attendai.school.teacher.service;

import com.attendai.school.teacher.dto.AssignUserToTeacherRequest;
import com.attendai.school.teacher.dto.ChangeTeacherStatusRequest;
import com.attendai.school.teacher.dto.CreateTeacherRequest;
import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.dto.TeacherSummaryResponse;
import com.attendai.school.teacher.dto.UpdateTeacherRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {

    TeacherResponse createTeacher(Long schoolId, CreateTeacherRequest request);

    TeacherResponse findById(Long schoolId, Long id);

    Page<TeacherSummaryResponse> listTeachers(Long schoolId, String search, Pageable pageable);

    TeacherResponse updateTeacher(Long schoolId, Long id, UpdateTeacherRequest request);

    TeacherResponse changeStatus(Long schoolId, Long id, ChangeTeacherStatusRequest request);

    TeacherResponse assignUser(Long schoolId, Long id, AssignUserToTeacherRequest request);

    TeacherResponse removeUser(Long schoolId, Long id);

    void deleteTeacher(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by school-teacher-assignment (Round 3+)
    // -------------------------------------------------------------------------

    /**
     * Returns true if a teacher with the given id exists and has ACTIVE status.
     * ON_LEAVE and INACTIVE are treated as not active for assignment purposes.
     */
    boolean isActive(Long id);

    /** Returns true if a teacher record with the given id exists (any status). */
    boolean existsById(Long id);
}
