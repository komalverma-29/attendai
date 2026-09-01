package com.attendai.school.student.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.person.service.PersonService;
import com.attendai.core.role.dto.AssignRoleRequest;
import com.attendai.core.role.service.RoleService;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.service.UserService;
import com.attendai.school.school.service.SchoolService;
import com.attendai.school.student.dto.AssignUserToStudentRequest;
import com.attendai.school.student.dto.ChangeStudentStatusRequest;
import com.attendai.school.student.dto.EnrollStudentRequest;
import com.attendai.school.student.dto.StudentResponse;
import com.attendai.school.student.dto.StudentSummaryResponse;
import com.attendai.school.student.dto.UpdateStudentRequest;
import com.attendai.school.student.entity.SchoolStudent;
import com.attendai.school.student.entity.StudentStatus;
import com.attendai.school.student.exception.StudentNotFoundException;
import com.attendai.school.student.mapper.SchoolStudentMapper;
import com.attendai.school.student.repository.SchoolStudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    /** Terminal statuses — re-enrollment requires a new record. */
    private static final Set<StudentStatus> TERMINAL_STATUSES =
            EnumSet.of(StudentStatus.TRANSFERRED, StudentStatus.GRADUATED);

    private static final String MODULE             = "school";
    private static final String SCHOOL_STUDENT_ROLE = "SCHOOL_STUDENT";

    private final SchoolStudentRepository studentRepository;
    private final SchoolStudentMapper     studentMapper;
    private final SchoolService           schoolService;
    private final PersonService           personService;
    private final UserService             userService;
    private final RoleService             roleService;
    private final AuditService            auditService;

    // -------------------------------------------------------------------------
    // Enroll
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse enrollStudent(Long schoolId, EnrollStudentRequest request) {
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException(
                    "Person with id " + request.getPersonId() + " was not found");
        }
        if (studentRepository.existsByPersonIdAndSchoolId(request.getPersonId(), schoolId)) {
            throw new ResourceAlreadyExistsException(
                    "Person with id " + request.getPersonId()
                    + " is already enrolled in school " + schoolId);
        }
        if (studentRepository.existsBySchoolIdAndAdmissionNumber(
                schoolId, request.getAdmissionNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Admission number '" + request.getAdmissionNumber()
                    + "' already exists in school " + schoolId);
        }

        SchoolStudent student = SchoolStudent.builder()
                .schoolId(schoolId)
                .personId(request.getPersonId())
                .admissionNumber(request.getAdmissionNumber())
                .enrollmentDate(request.getEnrollmentDate())
                .bloodGroup(request.getBloodGroup())
                .guardianName(request.getGuardianName())
                .guardianPhone(request.getGuardianPhone())
                .guardianEmail(request.getGuardianEmail())
                .notes(request.getNotes())
                .status(StudentStatus.ACTIVE)
                .build();

        SchoolStudent saved = studentRepository.save(student);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_ENROLLED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId
                         + ",\"personId\":" + request.getPersonId() + "}")
                .build());

        return studentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StudentResponse findById(Long schoolId, Long id) {
        return studentMapper.toResponse(requireStudent(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> listStudents(Long schoolId, String search,
                                                      Pageable pageable) {
        String norm = (search != null && !search.isBlank()) ? search.trim() : null;
        return studentRepository.findBySchoolIdAndSearch(schoolId, norm, pageable)
                .map(studentMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse updateStudent(Long schoolId, Long id, UpdateStudentRequest request) {
        SchoolStudent student = requireStudent(schoolId, id);

        if (request.getBloodGroup()   != null) student.setBloodGroup(request.getBloodGroup());
        if (request.getGuardianName() != null) student.setGuardianName(request.getGuardianName());
        if (request.getGuardianPhone()!= null) student.setGuardianPhone(request.getGuardianPhone());
        if (request.getGuardianEmail()!= null) student.setGuardianEmail(request.getGuardianEmail());
        if (request.getNotes()        != null) student.setNotes(request.getNotes());

        SchoolStudent saved = studentRepository.save(student);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_UPDATED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(id)).build());

        return studentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status change
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse changeStatus(Long schoolId, Long id,
                                         ChangeStudentStatusRequest request) {
        SchoolStudent student = requireStudent(schoolId, id);

        // BR-STU: cannot transition out of a terminal status
        if (TERMINAL_STATUSES.contains(student.getStatus())) {
            throw new ValidationException(
                    "Cannot change status of a student in terminal state: "
                    + student.getStatus() + ". Re-enrollment is required.");
        }

        student.setStatus(request.getStatus());
        SchoolStudent saved = studentRepository.save(student);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_STATUS_CHANGED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return studentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // User linking
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public StudentResponse assignUser(Long schoolId, Long id,
                                       AssignUserToStudentRequest request) {
        SchoolStudent student = requireStudent(schoolId, id);

        if (student.getUserId() != null) {
            throw new ValidationException(
                    "Student already has a user account linked. Remove it first.");
        }
        validateUser(request.getUserId(), student.getPersonId());

        if (studentRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "User with id " + request.getUserId()
                    + " is already linked to another student");
        }

        student.setUserId(request.getUserId());
        SchoolStudent saved = studentRepository.save(student);
        assignStudentRole(request.getUserId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_USER_ASSIGNED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(id))
                .details("{\"userId\":" + request.getUserId() + "}")
                .build());

        return studentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public StudentResponse removeUser(Long schoolId, Long id) {
        SchoolStudent student = requireStudent(schoolId, id);
        if (student.getUserId() == null) {
            throw new ValidationException("Student does not have a linked user account");
        }
        Long oldUserId = student.getUserId();
        student.setUserId(null);
        SchoolStudent saved = studentRepository.save(student);
        revokeStudentRole(oldUserId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_USER_REMOVED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(id)).build());

        return studentMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteStudent(Long schoolId, Long id) {
        SchoolStudent student = requireStudent(schoolId, id);

        // BR-STU-06: guard against active daily-attendance records.
        // school-daily-attendance is a future module — no attendance records possible yet.

        if (student.getUserId() != null) {
            revokeStudentRole(student.getUserId());
        }
        student.softDelete();
        studentRepository.save(student);

        auditService.log(AuditEventRequest.builder()
                .actionCode("STUDENT_DELETED")
                .module(MODULE).resourceType("SchoolStudent")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return studentRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse findById(Long schoolId, Long studentId, boolean throwIfNotFound) {
        return studentRepository.findById(studentId)
                .filter(s -> s.getSchoolId().equals(schoolId))
                .map(studentMapper::toResponse)
                .orElseThrow(() -> throwIfNotFound
                        ? new StudentNotFoundException(studentId)
                        : new StudentNotFoundException(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Long findByPersonId(Long personId, Long schoolId) {
        return studentRepository.findByPersonIdAndSchoolId(personId, schoolId)
                .map(SchoolStudent::getId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No student record found for personId=" + personId
                        + " in schoolId=" + schoolId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return studentRepository.findById(id)
                .map(s -> StudentStatus.ACTIVE.equals(s.getStatus()))
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolStudent requireStudent(Long schoolId, Long id) {
        SchoolStudent s = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        if (!s.getSchoolId().equals(schoolId)) throw new StudentNotFoundException(id);
        return s;
    }

    private void validateUser(Long userId, Long expectedPersonId) {
        var userAuth = userService.findByIdForAuth(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " was not found"));
        if (!UserStatus.ACTIVE.name().equals(userAuth.status())) {
            throw new ValidationException("User with id " + userId + " is not ACTIVE");
        }
        var userResponse = userService.findById(userId);
        if (!expectedPersonId.equals(userResponse.getPersonId())) {
            throw new ValidationException(
                    "User and Person do not belong to the same person record");
        }
    }

    private void assignStudentRole(Long userId) {
        roleService.findByCode(SCHOOL_STUDENT_ROLE).ifPresent(role -> {
            try {
                AssignRoleRequest req = new AssignRoleRequest();
                req.setRoleId(role.getId());
                roleService.assignRoleToUser(userId, req);
            } catch (Exception e) {
                log.warn("Could not assign {} to userId={}: {}",
                        SCHOOL_STUDENT_ROLE, userId, e.getMessage());
            }
        });
    }

    private void revokeStudentRole(Long userId) {
        roleService.findByCode(SCHOOL_STUDENT_ROLE).ifPresent(role -> {
            try {
                roleService.removeRoleFromUser(userId, role.getId());
            } catch (Exception e) {
                log.warn("Could not revoke {} from userId={}: {}",
                        SCHOOL_STUDENT_ROLE, userId, e.getMessage());
            }
        });
    }
}
