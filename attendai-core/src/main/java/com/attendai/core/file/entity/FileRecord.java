package com.attendai.core.file.entity;

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
 * Metadata record for an uploaded file.
 *
 * <p>The raw {@code storageKey} (internal storage path/object key) is stored
 * here but is <strong>never exposed in any API response</strong> — doing so
 * would allow direct storage access bypassing access controls.
 *
 * <p>Soft-deleted files are no longer accessible but their metadata is
 * retained for audit purposes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class FileRecord extends SoftDeletableEntity {

    /** Original filename as provided by the uploader (sanitised, max 255). */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * Unique storage key in the backend (UUID-based path).
     * NEVER exposed in any API response.
     */
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    /** MIME type, e.g. "image/jpeg". */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** File size in bytes. */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 10)
    @Builder.Default
    private FileVisibility visibility = FileVisibility.PRIVATE;

    /** FK → users(id). The user who uploaded this file. */
    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;

    /**
     * Module that owns this file, e.g. "core-face", "school".
     * Used for filtering and ownership tracking.
     */
    @Column(name = "module", length = 50)
    private String module;
}
