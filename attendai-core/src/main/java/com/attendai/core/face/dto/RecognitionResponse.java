package com.attendai.core.face.dto;

import lombok.Builder;
import lombok.Getter;

/** Response returned by the face recognition endpoint. */
@Getter
@Builder
public class RecognitionResponse {
    private final boolean matched;
    private final Long    personId;
    private final Long    faceProfileId;
    private final float   confidence;
    private final boolean livenessCheckPassed;
}
