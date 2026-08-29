package com.attendai.core.face.service;

import lombok.Builder;
import lombok.Getter;

/**
 * Result returned by {@link FaceRecognitionEngine#findBestMatch}.
 */
@Getter
@Builder
public class RecognitionResult {

    /** True when a person was matched above the confidence threshold. */
    private final boolean matched;

    /** Matched person's ID. Null when {@code matched} is false. */
    private final Long personId;

    /** Matched face profile's ID. Null when {@code matched} is false. */
    private final Long faceProfileId;

    /** Similarity confidence score in range 0.0 – 1.0. */
    private final float confidence;
}
