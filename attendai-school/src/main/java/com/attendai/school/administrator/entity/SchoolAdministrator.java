package com.attendai.school.administrator.entity;

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

/**
 * School administrator entity.
 *
 * <p>Links a Core {@code Person} (identity) and a Core {@code User} (login credentials)
 * to a specific school. The {@code User} is mandatory for administrators — they always
 * need platform access.
 *
 * <p>A school must always have at least one ACTIVE administrator.
 * Deactivating or deleting the last active administrator is rejected by the service.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_administrators")
public class SchoolAdministrator extends SoftDeletableEntity {

    /** FK → school_schools(id). NOT NULL. Set once at creation. */
    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    /** FK → persons(id). NOT NULL. Set once at creation. */
    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    /** FK → users(id). NOT NULL. Must be ACTIVE at time of creation. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    /** Optional role, e.g. "Principal", "Vice Principal". Max 100 chars. */
    @Column(name = "designation", length = 100)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AdministratorStatus status = AdministratorStatus.ACTIVE;

    @Column(name = "notes", length = 500)
    private String notes;
}
