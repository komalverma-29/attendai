package com.attendai.core.face.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Face image response.
 * {@code embeddingVector} is intentionally absent — it must never be exposed via API.
 */
@Getter
@Builder
public class FaceImageResponse {
    private final Long          id;
    private final Long          faceProfileId;
    private final Long          fileId;
    private final LocalDateTime capturedAt;
    private final LocalDateTime createdAt;
}
