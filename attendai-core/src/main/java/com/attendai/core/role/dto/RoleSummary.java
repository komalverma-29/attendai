package com.attendai.core.role.dto;

/**
 * Lightweight role summary used when building JWT claims.
 *
 * @param id   the role's surrogate ID
 * @param code the role code, e.g. "SCHOOL_ADMIN"
 */
public record RoleSummary(Long id, String code) {}
