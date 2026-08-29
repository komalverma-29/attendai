package com.attendai.core.face.entity;

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
 * A face profile belonging to a single person.
 *
 * <p>Each person may have at most one non-deleted face profile at a time.
 * {@code imageCount} is a denormalized count of active (non-deleted) face images —
 * it is incremented and decremented by the service layer to avoid re-querying.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_profiles")
public class FaceProfile extends SoftDeletableEntity {

    /** FK → persons(id). Set once at creation; never changed. */
    @Column(name = "person_id", nullable = false, updatable = false)
    private Long personId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FaceProfileStatus status = FaceProfileStatus.PENDING;

    /** Denormalized count of active (non-deleted) face images. */
    @Column(name = "image_count", nullable = false)
    @Builder.Default
    private int imageCount = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
