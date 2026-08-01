package com.attendai.core.user.dto;

/**
 * Lightweight projection of a user record used exclusively by {@code core-auth}
 * during authentication.
 *
 * Only the fields required to authenticate a user are exposed here.
 * Full user details (name, email, etc.) are never exposed through this projection.
 *
 * The {@code passwordHash} field is intentionally included because this DTO is
 * internal — it is only passed between Spring beans and never serialised to JSON.
 */
public record UserAuthProjection(
        Long id,
        String email,
        String passwordHash,
        String status,
        boolean mustChangePassword
) {
    /** Returns true when this user account can authenticate. */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
