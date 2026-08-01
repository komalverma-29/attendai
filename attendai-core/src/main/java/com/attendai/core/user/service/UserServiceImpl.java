package com.attendai.core.user.service;

import com.attendai.core.audit.dto.AuditEventRequest;
import com.attendai.core.audit.service.AuditService;
import com.attendai.core.common.constants.AttendAIConstants;
import com.attendai.core.common.exception.ResourceAlreadyExistsException;
import com.attendai.core.common.exception.ResourceNotFoundException;
import com.attendai.core.common.exception.ValidationException;
import com.attendai.core.user.dto.ChangePasswordRequest;
import com.attendai.core.user.dto.ChangeStatusRequest;
import com.attendai.core.user.dto.CreateUserRequest;
import com.attendai.core.user.dto.UpdateUserRequest;
import com.attendai.core.user.dto.UserAuthProjection;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.dto.UserSummaryResponse;
import com.attendai.core.user.entity.User;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.exception.UserAlreadyExistsException;
import com.attendai.core.user.exception.UserNotFoundException;
import com.attendai.core.user.mapper.UserMapper;
import com.attendai.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Full implementation of {@link UserService}.
 *
 * All write operations are audit-logged via {@link AuditService}.
 * {@code passwordHash} is never returned in any DTO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository  userRepository;
    private final UserMapper      userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService    auditService;

    /**
     * Allowed status transitions.
     * Key = current status; Value = set of valid target statuses.
     */
    private static final Map<UserStatus, Set<UserStatus>> ALLOWED_TRANSITIONS = Map.of(
            UserStatus.ACTIVE,    EnumSet.of(UserStatus.INACTIVE, UserStatus.SUSPENDED, UserStatus.LOCKED),
            UserStatus.INACTIVE,  EnumSet.of(UserStatus.ACTIVE),
            UserStatus.SUSPENDED, EnumSet.of(UserStatus.ACTIVE),
            UserStatus.LOCKED,    EnumSet.of(UserStatus.ACTIVE)
    );

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Email uniqueness — checked across ALL users including soft-deleted
        if (userRepository.existsByEmailIncludingDeleted(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }
        // Username uniqueness — among non-deleted users only
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username", request.getUsername());
        }
        // One-user-per-person rule
        if (userRepository.findByPersonId(request.getPersonId()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "User account for person with id " + request.getPersonId() + " already exists");
        }

        User user = User.builder()
                .personId(request.getPersonId())
                .email(request.getEmail().trim().toLowerCase())
                .username(request.getUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .status(UserStatus.ACTIVE)
                .mustChangePassword(request.isMustChangePassword())
                .build();

        User saved = userRepository.save(user);
        log.info("User created | userId={} email=[REDACTED]", saved.getId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_CREATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(saved.getId()))
                .build());

        return userMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userMapper.toResponse(requireUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("username", username));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(UserStatus status, String search, Pageable pageable) {
        return userRepository.findByFilters(status, search, pageable)
                .map(userMapper::toSummaryResponse);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = requireUser(id);

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail()) &&
                    userRepository.existsByEmailIncludingDeleted(newEmail)) {
                throw new UserAlreadyExistsException("email", newEmail);
            }
            user.setEmail(newEmail);
        }

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equals(user.getUsername()) &&
                    userRepository.existsByUsername(newUsername)) {
                throw new UserAlreadyExistsException("username", newUsername);
            }
            user.setUsername(newUsername);
        }

        if (request.getMustChangePassword() != null) {
            user.setMustChangePassword(request.getMustChangePassword());
        }

        User saved = userRepository.save(user);
        log.info("User updated | userId={}", saved.getId());

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_UPDATED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(saved.getId()))
                .build());

        return userMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Status management
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UserResponse changeStatus(Long id, ChangeStatusRequest request) {
        User user = requireUser(id);
        UserStatus from = user.getStatus();
        UserStatus to   = request.getStatus();

        Set<UserStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            log.warn("Invalid status transition {} → {} for userId={}", from, to, id);
            throw new ValidationException(
                    "Cannot transition user status from " + from + " to " + to);
        }

        user.setStatus(to);
        User saved = userRepository.save(user);
        log.info("User status changed {} → {} | userId={}", from, to, id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_STATUS_CHANGED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(id))
                .details("{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}")
                .build());

        return userMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Password management (self-service via HTTP)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = requireUser(id);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password changed via self-service | userId={}", id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_PASSWORD_CHANGED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Soft delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = requireUser(id);
        user.softDelete();
        userRepository.save(user);
        log.info("User soft-deleted | userId={}", id);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_DELETED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(id))
                .build());
    }

    // -------------------------------------------------------------------------
    // Internal API for core-auth
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthProjection> findByEmailForAuth(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> new UserAuthProjection(
                        u.getId(),
                        u.getEmail(),
                        u.getPasswordHash(),
                        u.getStatus().name(),
                        u.isMustChangePassword()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthProjection> findByIdForAuth(Long id) {
        return userRepository.findById(id)
                .map(u -> new UserAuthProjection(
                        u.getId(),
                        u.getEmail(),
                        u.getPasswordHash(),
                        u.getStatus().name(),
                        u.isMustChangePassword()));
    }

    @Override
    @Transactional
    public void updatePasswordHash(Long userId, String newPasswordHash) {
        User user = requireUser(userId);
        user.setPasswordHash(newPasswordHash);
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_PASSWORD_CHANGED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(userId))
                .details("{\"source\":\"password_reset\"}")
                .build());
    }

    @Override
    @Transactional
    public void updateLastLoginAt(Long userId) {
        userRepository.updateLastLoginAt(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void lockUser(Long userId) {
        userRepository.lockUser(userId);
        log.warn("User account locked | userId={}", userId);

        auditService.log(AuditEventRequest.builder()
                .actionCode("USER_STATUS_CHANGED")
                .module(AttendAIConstants.MODULE_CORE)
                .resourceType("User")
                .resourceId(String.valueOf(userId))
                .details("{\"from\":\"ACTIVE\",\"to\":\"LOCKED\",\"reason\":\"repeated_login_failures\"}")
                .build());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
