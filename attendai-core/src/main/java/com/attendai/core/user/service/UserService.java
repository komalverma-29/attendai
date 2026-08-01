package com.attendai.core.user.service;

import com.attendai.core.user.dto.ChangePasswordRequest;
import com.attendai.core.user.dto.ChangeStatusRequest;
import com.attendai.core.user.dto.CreateUserRequest;
import com.attendai.core.user.dto.UpdateUserRequest;
import com.attendai.core.user.dto.UserResponse;
import com.attendai.core.user.dto.UserSummaryResponse;
import com.attendai.core.user.entity.UserStatus;
import com.attendai.core.user.dto.UserAuthProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Core user account service.
 *
 * Manages user lifecycle: create, read, update, status transitions,
 * password management, and soft delete.
 */
public interface UserService {

    // -------------------------------------------------------------------------
    // CRUD
    // -------------------------------------------------------------------------

    UserResponse createUser(CreateUserRequest request);

    UserResponse findById(Long id);

    UserResponse findByUsername(String username);

    Page<UserSummaryResponse> listUsers(UserStatus status, String search, Pageable pageable);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    // -------------------------------------------------------------------------
    // Status management
    // -------------------------------------------------------------------------

    UserResponse changeStatus(Long id, ChangeStatusRequest request);

    // -------------------------------------------------------------------------
    // Password management
    // -------------------------------------------------------------------------

    void changePassword(Long id, ChangePasswordRequest request);

    // -------------------------------------------------------------------------
    // Internal API consumed by core-auth (not exposed via HTTP)
    // -------------------------------------------------------------------------

    Optional<UserAuthProjection> findByEmailForAuth(String email);

    Optional<UserAuthProjection> findByIdForAuth(Long id);

    void updatePasswordHash(Long userId, String newPasswordHash);

    void updateLastLoginAt(Long userId);

    void lockUser(Long userId);
}
