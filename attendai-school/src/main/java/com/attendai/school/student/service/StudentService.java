package com.attendai.school.student.service;

import com.attendai.school.student.dto.AssignUserToStudentRequest;
import com.attendai.school.student.dto.ChangeStudentStatusRequest;
import com.attendai.school.student.dto.EnrollStudentRequest;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.dto.StudentSummaryResponse;
import com.attendai.school.student.dto.UpdateStudentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {

    StudentResponse enrollStudent(Long schoolId, EnrollStudentRequest request);

    StudentResponse findById(Long schoolId, Long id);

    Page<StudentSummaryResponse> listStudents(Long schoolId, String search, Pageable pageable);

    StudentResponse updateStudent(Long schoolId, Long id, UpdateStudentRequest request);

    StudentResponse changeStatus(Long schoolId, Long id, ChangeStudentStatusRequest request);

    StudentResponse assignUser(Long schoolId, Long id, AssignUserToStudentRequest request);

    StudentResponse removeUser(Long schoolId, Long id);

    void deleteStudent(Long schoolId, Long id);

    // -------------------------------------------------------------------------
    // Internal APIs — consumed by future Round-2+ modules (school-section, etc.)
    // -------------------------------------------------------------------------

    boolean existsById(Long id);

    StudentResponse findById(Long schoolId, Long studentId, boolean throwIfNotFound);

    Long findByPersonId(Long personId, Long schoolId);

    boolean isActive(Long id);
}
