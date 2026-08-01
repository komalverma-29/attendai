package com.attendai.core.user.dto;

import com.attendai.core.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Full user response DTO.
 * {@code passwordHash} is intentionally absent — it must never appear in any response.
 */
@Getter
@Builder
public class UserResponse {

    private final Long id;
    private final Long personId;
    private final String email;
    private final String username;
    private final UserStatus status;
    private final boolean mustChangePassword;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
