package com.attendai.core.user.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * User account entity. Holds login credentials and account lifecycle state.
 *
 * A user is always linked to exactly one {@code Person} via {@code personId}.
 * The link is stored as a raw FK — no JPA relationship to avoid cross-module
 * coupling between core-user and core-person.
 *
 * The {@code passwordHash} field is NEVER included in any DTO or API response.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "passwordHash")
@Entity
@Table(name = "users")
public class User extends SoftDeletableEntity {

    /** FK → persons(id). Set once at creation; never changed. */
    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt hash. NEVER exposed in any response DTO. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
