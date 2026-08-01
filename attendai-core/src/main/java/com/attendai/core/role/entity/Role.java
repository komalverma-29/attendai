package com.attendai.core.role.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Platform role entity.
 *
 * Role codes are uppercase, unique, and immutable after creation.
 * System roles ({@code isSystem = true}) cannot be deleted.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role extends SoftDeletableEntity {

    /** Unique role code, e.g. SYSTEM_ADMIN. Immutable after creation. */
    @Column(name = "code", nullable = false, unique = true, length = 100, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** System roles cannot be deleted. */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;
}
