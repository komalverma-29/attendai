package com.attendai.school.administrator.service;

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
import com.attendai.school.administrator.dto.AdministratorResponse;
import com.attendai.school.administrator.dto.AdministratorSummaryResponse;
import com.attendai.school.administrator.dto.ChangeAdministratorStatusRequest;
import com.attendai.school.administrator.dto.CreateAdministratorRequest;
import com.attendai.school.administrator.dto.UpdateAdministratorRequest;
import com.attendai.school.administrator.entity.AdministratorStatus;
import com.attendai.school.administrator.entity.SchoolAdministrator;
import com.attendai.school.administrator.exception.AdministratorNotFoundException;
import com.attendai.school.administrator.mapper.SchoolAdministratorMapper;
import com.attendai.school.administrator.repository.SchoolAdministratorRepository;
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
public class AdministratorServiceImpl implements AdministratorService {

    private static final String MODULE        = "school";
    private static final String SCHOOL_ADMIN_ROLE = "SCHOOL_ADMIN";

    private final SchoolAdministratorRepository adminRepository;
    private final SchoolAdministratorMapper     adminMapper;
    private final SchoolService                 schoolService;
    private final PersonService                 personService;
    private final UserService                   userService;
    private final RoleService                   roleService;
    private final AuditService                  auditService;

    @Override
    @Transactional
    public AdministratorResponse createAdministrator(Long schoolId,
                                                      CreateAdministratorRequest request) {
        // Validate school is active
        if (!schoolService.isActive(schoolId)) {
            throw new ValidationException("School with id " + schoolId + " is not active");
        }
        // Validate person exists
        if (!personService.existsById(request.getPersonId())) {
            throw new ResourceNotFoundException(
                    "Person with id " + request.getPersonId() + " was not found");
        }
        // Validate user exists and is ACTIVE
        var userAuth = userService.findByIdForAuth(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + request.getUserId() + " was not found"));
        if (!UserStatus.ACTIVE.name().equals(userAuth.status())) {
            throw new ValidationException(
                    "User with id " + request.getUserId() + " is not ACTIVE");
        }
        // Validate person-user link (user.personId must equal request.personId)
        // We check via UserResponse to avoid coupling — the user entity has personId
        var userResponse = userService.findById(request.getUserId());
        if (!request.getPersonId().equals(userResponse.getPersonId())) {
            throw new ValidationException(
                    "User and Person do not belong to the same person record");
        }
        // Enforce one-admin-per-person-per-school
        if (adminRepository.existsByPersonIdAndSchoolId(request.getPersonId(), schoolId)) {
            throw new ResourceAlreadyExistsException(
                    "Person with id " + request.getPersonId()
                    + " is already an administrator of school " + schoolId);
        }
        // Enforce one-user-per-admin
        if (adminRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "User with id " + request.getUserId()
                    + " is already linked to an administrator record");
        }

        SchoolAdministrator admin = SchoolAdministrator.builder()
                .schoolId(schoolId)
                .personId(request.getPersonId())
                .userId(request.getUserId())
                .designation(request.getDesignation())
                .status(AdministratorStatus.ACTIVE)
                .notes(request.getNotes())
                .build();
        SchoolAdministrator saved = adminRepository.save(admin);

        // Assign SCHOOL_ADMIN role
        assignRole(request.getUserId());

        log.info("Administrator created | schoolId={} adminId={}", schoolId, saved.getId());
        auditService.log(AuditEventRequest.builder()
                .actionCode("ADMINISTRATOR_CREATED")
                .module(MODULE)
                .resourceType("SchoolAdministrator")
                .resourceId(String.valueOf(saved.getId()))
                .details("{\"schoolId\":" + schoolId + ",\"personId\":" + request.getPersonId() + "}")
                .build());

        return adminMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdministratorResponse findById(Long schoolId, Long id) {
        return adminMapper.toResponse(requireAdmin(schoolId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdministratorSummaryResponse> listAdministrators(Long schoolId,
                                                                  AdministratorStatus status,
                                                                  Pageable pageable) {
        return adminRepository.findBySchoolIdAndStatus(schoolId, status, pageable)
                .map(adminMapper::toSummaryResponse);
    }

    @Override
    @Transactional
    public AdministratorResponse updateAdministrator(Long schoolId, Long id,
                                                      UpdateAdministratorRequest request) {
        SchoolAdministrator admin = requireAdmin(schoolId, id);
        if (request.getDesignation() != null) admin.setDesignation(request.getDesignation());
        if (request.getNotes()       != null) admin.setNotes(request.getNotes());
        SchoolAdministrator saved = adminRepository.save(admin);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ADMINISTRATOR_UPDATED")
                .module(MODULE)
                .resourceType("SchoolAdministrator")
                .resourceId(String.valueOf(id))
                .build());

        return adminMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AdministratorResponse changeStatus(Long schoolId, Long id,
                                               ChangeAdministratorStatusRequest request) {
        SchoolAdministrator admin = requireAdmin(schoolId, id);

        if (request.getStatus() == AdministratorStatus.INACTIVE) {
            // Last-admin guard
            long activeCount = adminRepository.countBySchoolIdAndStatus(
                    schoolId, AdministratorStatus.ACTIVE);
            if (activeCount <= 1) {
                throw new ValidationException(
                        "Cannot deactivate the last active administrator of school " + schoolId);
            }
            // Revoke role when deactivating
            revokeRole(admin.getUserId());
        } else if (request.getStatus() == AdministratorStatus.ACTIVE
                   && admin.getStatus() == AdministratorStatus.INACTIVE) {
            // Re-assign role when re-activating
            assignRole(admin.getUserId());
        }

        admin.setStatus(request.getStatus());
        SchoolAdministrator saved = adminRepository.save(admin);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ADMINISTRATOR_STATUS_CHANGED")
                .module(MODULE)
                .resourceType("SchoolAdministrator")
                .resourceId(String.valueOf(id))
                .details("{\"status\":\"" + request.getStatus() + "\"}")
                .build());

        return adminMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAdministrator(Long schoolId, Long id) {
        SchoolAdministrator admin = requireAdmin(schoolId, id);

        // Last-admin guard
        long activeCount = adminRepository.countBySchoolIdAndStatus(
                schoolId, AdministratorStatus.ACTIVE);
        if (admin.getStatus() == AdministratorStatus.ACTIVE && activeCount <= 1) {
            throw new ValidationException(
                    "Cannot delete the last active administrator of school " + schoolId);
        }

        // Revoke SCHOOL_ADMIN role
        revokeRole(admin.getUserId());

        admin.softDelete();
        adminRepository.save(admin);

        auditService.log(AuditEventRequest.builder()
                .actionCode("ADMINISTRATOR_DELETED")
                .module(MODULE)
                .resourceType("SchoolAdministrator")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SchoolAdministrator requireAdmin(Long schoolId, Long id) {
        SchoolAdministrator admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdministratorNotFoundException(id));
        if (!admin.getSchoolId().equals(schoolId)) {
            throw new AdministratorNotFoundException(id);
        }
        return admin;
    }

    private void assignRole(Long userId) {
        roleService.findByCode(SCHOOL_ADMIN_ROLE).ifPresent(role -> {
            try {
                AssignRoleRequest req = new AssignRoleRequest();
                req.setRoleId(role.getId());
                roleService.assignRoleToUser(userId, req);
            } catch (Exception e) {
                // Role may already be assigned — log and continue
                log.warn("Could not assign {} role to userId={}: {}", SCHOOL_ADMIN_ROLE, userId, e.getMessage());
            }
        });
    }

    private void revokeRole(Long userId) {
        roleService.findByCode(SCHOOL_ADMIN_ROLE).ifPresent(role -> {
            try {
                roleService.removeRoleFromUser(userId, role.getId());
            } catch (Exception e) {
                log.warn("Could not revoke {} role from userId={}: {}", SCHOOL_ADMIN_ROLE, userId, e.getMessage());
            }
        });
    }
}
