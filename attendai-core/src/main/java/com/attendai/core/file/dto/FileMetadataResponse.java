package com.attendai.core.file.dto;

import com.attendai.core.file.entity.FileVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Full file metadata response.
 * {@code storageKey} is intentionally absent — it must never be exposed.
 */
@Getter
@Builder
public class FileMetadataResponse {
    private final Long           id;
    private final String         originalName;
    private final String         contentType;
    private final long           sizeBytes;
    private final FileVisibility visibility;
    private final String         module;
    private final Long           uploadedByUserId;
    private final LocalDateTime  createdAt;
    private final LocalDateTime  updatedAt;
}
