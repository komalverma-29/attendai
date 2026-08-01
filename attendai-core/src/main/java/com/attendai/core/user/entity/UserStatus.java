package com.attendai.core.user.entity;

/**
 * Represents the lifecycle state of a user account.
 *
 * Only {@code ACTIVE} accounts can authenticate.
 * Status transitions are validated in {@link com.attendai.core.user.service.UserServiceImpl}.
 */
public enum UserStatus {

    /** Account is fully operational and can authenticate. */
    ACTIVE,

    /** Account has been manually deactivated by an administrator. Cannot authenticate. */
    INACTIVE,

    /** Account is temporarily suspended. Cannot authenticate. */
    SUSPENDED,

    /** Account is locked (typically after repeated login failures). Cannot authenticate. */
    LOCKED;

    /** Returns true when this status allows authentication. */
    public boolean canAuthenticate() {
        return this == ACTIVE;
    }
}
