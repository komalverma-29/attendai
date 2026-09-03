package com.attendai.school.teacher.service;

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
import com.attendai.school.teacher.dto.AssignUserToTeacherRequest;
import com.attendai.school.teacher.dto.ChangeTeacherStatusRequest;
import com.attendai.school.teacher.dto.CreateTeacherRequest;
import com.attendai.school.teacher.dto.TeacherResponse;
import com.attendai.school.teacher.dto.TeacherSummaryResponse;
import com.attendai.school.teacher.dto.UpdateTeacherRequest;
import com.attendai.school.teacher.entity.SchoolTeacher;
import com.attendai.school.teacher.entity.TeacherStatus;
import com.attendai.school.teacher.exception.TeacherNotFoundException;
import com.attendai.school.teacher.mapper.SchoolTeacherMapper;
import com.attendai.school.teacher.repository.SchoolTeacherRepository;
import com.attendai.school.school.service.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private static final String MODULE             = "school";
    private static final String SCHOOL_TEACHER_ROLE = "SCHOOL_TEACHER";

    private final SchoolTeacherRepository teacherRepository;
    private final SchoolTeacherMapper     teacherMapper;
    private final SchoolService           schoolService;
    private final PersonService           personService;
    private final UserService             userService;
    private final RoleService             roleService;
    private final AuditService            auditService;

    @Override
    @Transactional
    public TeacherResponse createTeacher(Long schoolId, CreateTeacherRequest request) {
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException(
                    "Person with id " + request.getPersonId() + " was not found");
        }
        if (teacherRepository.existsByPersonIdAndSchoolId(request.getPersonId(), schoolId)) {
            throw new ResourceAlreadyExistsException(
                    "Person with id " + request.getPersonId()
                    + " is already a teacher in school " + schoolId);
        }
        if (request.getEmployeeCode() != null
                && teacherRepository.existsBySchoolIdAndEmployeeCode(
                        schoolId, request.getEmployeeCode())) {
            throw new ResourceAlreadyExistsException(
                    "Employee code '" + request.getEmployeeCode()
                    + "' already exists in school " + schoolId);
        }

        Long userId = request.getUserId();
        if (userId != null) {
            validateAndLinkUser(userId, request.getPersonId());
        }

        SchoolTeacher teacher = SchoolTeacher.builder()
                .schoolId(schoolId)
                .personId(request.getPersonId())
                .userId(userId)
                .employeeCode(request.getEmployeeCode())
                .designation(request.getDesignation())
                .qualification(request.getQualification())
                .department(request.getDepartment())
                .status(TeacherStatus.ACTIVE)
                .notes(request.getNotes())
                .joiningDate(request.getJoiningDate())
                .build();
        SchoolTeacher saved = teacherRepository.save(teacher);

        if (userId != null) {
            assignTeacherRole(userId);
        }

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_CREATED")
                .module(MODULE)
                .resourceType("SchoolTeacher")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId + ",\"personId\":" + request.getPersonId() + "}")
                .build());

        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse findById(Long schoolId, Long id) {
        return teacherMapper.toResponse(requireTeacher(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeacherSummaryResponse> listTeachers(Long schoolId, String search, Pageable pageable) {
        String normSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return teacherRepository.findBySchoolIdAndSearch(schoolId, normSearch, pageable)
                .map(teacherMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(Long schoolId, Long id, UpdateTeacherRequest request) {
        SchoolTeacher teacher = requireTeacher(schoolId, id);
        if (request.getEmployeeCode() != null) {
            if (!request.getEmployeeCode().equals(teacher.getEmployeeCode())
                    && teacherRepository.existsBySchoolIdAndEmployeeCode(
                            schoolId, request.getEmployeeCode())) {
                throw new ResourceAlreadyExistsException(
                        "Employee code '" + request.getEmployeeCode() + "' already exists");
            }
            teacher.setEmployeeCode(request.getEmployeeCode());
        }
        if (request.getDesignation()  != null) teacher.setDesignation(request.getDesignation());
        if (request.getQualification()!= null) teacher.setQualification(request.getQualification());
        if (request.getDepartment()   != null) teacher.setDepartment(request.getDepartment());
        if (request.getNotes()        != null) teacher.setNotes(request.getNotes());
        SchoolTeacher saved = teacherRepository.save(teacher);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_UPDATED")
                .module(MODULE).resourceType("SchoolTeacher")
                .resourceId(String.valueOf(id)).build());

        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TeacherResponse changeStatus(Long schoolId, Long id, ChangeTeacherStatusRequest request) {
        SchoolTeacher teacher = requireTeacher(schoolId, id);
        teacher.setStatus(request.getStatus());
        SchoolTeacher saved = teacherRepository.save(teacher);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_STATUS_CHANGED")
                .module(MODULE).resourceType("SchoolTeacher")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TeacherResponse assignUser(Long schoolId, Long id, AssignUserToTeacherRequest request) {
        SchoolTeacher teacher = requireTeacher(schoolId, id);
        if (teacher.getUserId() != null) {
            throw new ValidationException(
                    "Teacher already has a user account linked. Remove it first.");
        }
        validateAndLinkUser(request.getUserId(), teacher.getPersonId());
        if (teacherRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "User with id " + request.getUserId() + " is already linked to another teacher");
        }
        teacher.setUserId(request.getUserId());
        SchoolTeacher saved = teacherRepository.save(teacher);
        assignTeacherRole(request.getUserId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_USER_ASSIGNED")
                .module(MODULE).resourceType("SchoolTeacher")
                .resourceId(String.valueOf(id))
                .details("{\"userId\":" + request.getUserId() + "}")
                .build());

        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TeacherResponse removeUser(Long schoolId, Long id) {
        SchoolTeacher teacher = requireTeacher(schoolId, id);
        if (teacher.getUserId() == null) {
            throw new ValidationException("Teacher does not have a linked user account");
        }
        Long oldUserId = teacher.getUserId();
        teacher.setUserId(null);
        SchoolTeacher saved = teacherRepository.save(teacher);
        revokeTeacherRole(oldUserId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_USER_REMOVED")
                .module(MODULE).resourceType("SchoolTeacher")
                .resourceId(String.valueOf(id)).build());

        return teacherMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long schoolId, Long id) {
        SchoolTeacher teacher = requireTeacher(schoolId, id);

        // BR-TEACHER-07: guard against active timetable assignments.
        // school-teacher-assignment is a future module — no active assignments possible yet.

        if (teacher.getUserId() != null) {
            revokeTeacherRole(teacher.getUserId());
        }
        teacher.softDelete();
        teacherRepository.save(teacher);

        auditService.log(AuditEventRequest.builder()
                .actionCode("TEACHER_DELETED")
                .module(MODULE).resourceType("SchoolTeacher")
                .resourceId(String.valueOf(id)).build());
    }

    // -------------------------------------------------------------------------
    // Internal APIs
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long id) {
        return teacherRepository.findById(id)
                .map(t -> TeacherStatus.ACTIVE.equals(t.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return teacherRepository.existsById(id);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolTeacher requireTeacher(Long schoolId, Long id) {
        SchoolTeacher t = teacherRepository.findById(id)
                .orElseThrow(() -> new TeacherNotFoundException(id));
        if (!t.getSchoolId().equals(schoolId)) throw new TeacherNotFoundException(id);
        return t;
    }

    private void validateAndLinkUser(Long userId, Long expectedPersonId) {
        var userAuth = userService.findByIdForAuth(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " was not found"));
        if (!UserStatus.ACTIVE.name().equals(userAuth.status())) {
            throw new ValidationException("User with id " + userId + " is not ACTIVE");
        }
        var userResponse = userService.findById(userId);
        if (!expectedPersonId.equals(userResponse.getPersonId())) {
            throw new ValidationException("User and Person do not belong to the same person record");
        }
    }

    private void assignTeacherRole(Long userId) {
        roleService.findByCode(SCHOOL_TEACHER_ROLE).ifPresent(role -> {
            try {
                AssignRoleRequest req = new AssignRoleRequest();
                req.setRoleId(role.getId());
                roleService.assignRoleToUser(userId, req);
            } catch (Exception e) {
                log.warn("Could not assign {} to userId={}: {}", SCHOOL_TEACHER_ROLE, userId, e.getMessage());
            }
        });
    }

    private void revokeTeacherRole(Long userId) {
        roleService.findByCode(SCHOOL_TEACHER_ROLE).ifPresent(role -> {
            try {
                roleService.removeRoleFromUser(userId, role.getId());
            } catch (Exception e) {
                log.warn("Could not revoke {} from userId={}: {}", SCHOOL_TEACHER_ROLE, userId, e.getMessage());
            }
        });
    }
}
