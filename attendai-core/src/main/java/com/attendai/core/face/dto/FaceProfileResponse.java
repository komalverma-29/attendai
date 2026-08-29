package com.attendai.core.face.dto;

import com.attendai.core.face.entity.FaceProfileStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Face profile response.
 * Embedding vectors are intentionally absent from this and all other response DTOs.
 */
@Getter
@Builder
public class FaceProfileResponse {
    private final Long              id;
    private final Long              personId;
    private final FaceProfileStatus status;
    private final int               imageCount;
    private final String            notes;
    private final LocalDateTime     createdAt;
    private final LocalDateTime     updatedAt;
}
