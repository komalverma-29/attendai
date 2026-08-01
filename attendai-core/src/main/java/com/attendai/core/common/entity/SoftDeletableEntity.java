package com.attendai.core.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Abstract base class for JPA entities that support soft deletion.
 *
 * Extends {@link BaseEntity} and adds:
 * - isDeleted flag (BOOLEAN, default false)
 * - deletedAt timestamp (nullable DATETIME)
 *
 * The {@code @SQLRestriction} annotation ensures that Hibernate automatically
 * appends {@code is_deleted = false} to every query for this entity,
 * so soft-deleted records are never returned by standard repository methods.
 *
 * Hard deletion is only permitted for reference/lookup data or test teardown.
 */
@Getter
@Setter
@MappedSuperclass
@SQLRestriction("is_deleted = false")
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Marks this entity as soft-deleted by setting the {@code isDeleted} flag
     * and recording the current timestamp.
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
