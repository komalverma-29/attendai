package com.attendai.core.face.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
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
 * A single enrolled face image linked to a {@link FaceProfile}.
 *
 * <p>The raw image binary is stored in {@code core-file} (referenced by {@code fileId}).
 * The {@code embeddingVector} is a JSON array of floats extracted by the recognition
 * engine and is used for all similarity comparisons during recognition queries.
 *
 * <p>SECURITY: {@code embeddingVector} must never be exposed in any API response.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "face_images")
public class FaceImage extends SoftDeletableEntity {

    /** FK → face_profiles(id). */
    @Column(name = "face_profile_id", nullable = false, updatable = false)
    private Long faceProfileId;

    /** FK → files(id) in core-file. The raw image binary is stored there. */
    @Column(name = "file_id", nullable = false, updatable = false)
    private Long fileId;

    /**
     * JSON array of float values representing the face embedding vector.
     * Stored as TEXT. Must never be exposed in any API response.
     */
    @Column(name = "embedding_vector", nullable = false, columnDefinition = "TEXT")
    private String embeddingVector;

    /** When the image was originally captured by the station (nullable). */
    @Column(name = "captured_at")
    private LocalDateTime capturedAt;
}
