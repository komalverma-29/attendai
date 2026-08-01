package com.attendai.core.auth.repository;

import com.attendai.core.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Find a token by its SHA-256 hash. Used for all token lookups. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Count active (non-revoked, non-expired) tokens for a user. */
    @Query("""
            SELECT COUNT(r) FROM RefreshToken r
            WHERE r.userId = :userId
              AND r.isRevoked = false
              AND r.expiresAt > :now
            """)
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Find the oldest active token for a user — used to enforce max-sessions limit. */
    @Query("""
            SELECT r FROM RefreshToken r
            WHERE r.userId = :userId
              AND r.isRevoked = false
              AND r.expiresAt > :now
            ORDER BY r.createdAt ASC
            LIMIT 1
            """)
    Optional<RefreshToken> findOldestActiveByUserId(@Param("userId") Long userId,
                                                    @Param("now") LocalDateTime now);

    /** Revoke all active tokens for a user (used on password reset and token reuse detection). */
    @Modifying
    @Query("""
            UPDATE RefreshToken r
               SET r.isRevoked = true, r.revokedAt = :now
             WHERE r.userId = :userId
               AND r.isRevoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Delete expired tokens — used by the cleanup scheduled job. */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);

    /** Find all active tokens for a user — used for reuse detection logic. */
    @Query("""
            SELECT r FROM RefreshToken r
            WHERE r.userId = :userId
              AND r.isRevoked = false
            """)
    List<RefreshToken> findAllActiveByUserId(@Param("userId") Long userId);
}
