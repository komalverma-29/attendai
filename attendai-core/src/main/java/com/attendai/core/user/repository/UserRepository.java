package com.attendai.core.user.repository;

import com.attendai.core.user.entity.User;
import com.attendai.core.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for User entities.
 *
 * The {@code @SQLRestriction("is_deleted = false")} on {@link com.attendai.core.common.entity.SoftDeletableEntity}
 * automatically excludes soft-deleted users from all standard queries.
 *
 * For auth lookups, we query only active (non-deleted) users to prevent
 * soft-deleted accounts from authenticating.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email — used by core-auth during login.
     * The soft-delete filter is applied automatically via @SQLRestriction.
     */
    Optional<User> findByEmail(String email);

    /** Find user by username. */
    Optional<User> findByUsername(String username);

    /** Find user by personId — used to enforce one-user-per-person rule. */
    Optional<User> findByPersonId(Long personId);

    /** Check if an email already exists (includes soft-deleted for uniqueness check). */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
    boolean existsByEmailIncludingDeleted(@Param("email") String email);

    /** Check if a username exists among non-deleted users. */
    boolean existsByUsername(String username);

    /**
     * List users with optional status and search filters.
     * Search matches partial email or username (case-insensitive).
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR u.status = :status)
              AND (:search IS NULL
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> findByFilters(
            @Param("status") UserStatus status,
            @Param("search") String search,
            Pageable pageable);

    /** Update last login timestamp — lightweight write used by core-auth. */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :now WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Lock a user account — called by core-auth after repeated login failures. */
    @Modifying
    @Query("UPDATE User u SET u.status = 'LOCKED' WHERE u.id = :userId AND u.status = 'ACTIVE'")
    void lockUser(@Param("userId") Long userId);
}
