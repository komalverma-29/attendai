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
 * Persisted refresh token record.
 *
 * The raw token is NEVER stored. Only the SHA-256 hash of the token is persisted.
 * The raw token is returned to the client once at issuance and is not recoverable.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    /** The user this token belongs to. Raw FK — no JPA relationship to avoid cross-module coupling. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** SHA-256 hex hash of the raw token. Used as the lookup key. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    /** When this token expires (UTC). */
    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    /** Whether this token has been revoked. */
    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    /** When this token was revoked. Null if not revoked. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** Marks this token as revoked. */
    public void revoke() {
        this.isRevoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    /** Returns true when this token is active (not revoked and not expired). */
    public boolean isActive() {
        return !isRevoked && expiresAt.isAfter(LocalDateTime.now());
    }
}
