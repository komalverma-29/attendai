package com.attendai.core.auth.entity;

import com.attendai.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Single-use password reset token.
 *
 * The raw token is NEVER stored. Only the SHA-256 hash is persisted.
 * Tokens expire after the configured TTL (default: 1 hour) and are single-use.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean isUsed = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /** Marks this token as used. */
    public void markUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    /** Returns true when the token is still valid (not used and not expired). */
    public boolean isValid() {
        return !isUsed && expiresAt.isAfter(LocalDateTime.now());
    }
}
