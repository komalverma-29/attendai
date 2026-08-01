package com.attendai.core.auth.repository;

import com.attendai.core.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Invalidate any existing unused tokens for a user before issuing a new one. */
    @Modifying
    @Query("""
            UPDATE PasswordResetToken p
               SET p.isUsed = true, p.usedAt = :now
             WHERE p.userId = :userId
               AND p.isUsed = false
            """)
    int invalidateAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Delete expired tokens — used by the cleanup scheduled job. */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
