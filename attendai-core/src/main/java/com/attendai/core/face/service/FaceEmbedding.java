package com.attendai.core.face.service;

import lombok.Builder;
import lombok.Getter;

/**
 * A face embedding vector extracted from an image by the {@link FaceRecognitionEngine}.
 *
 * <p>The {@code vector} is a float array representing the facial features.
 * {@code faceProfileId} and {@code faceImageId} link back to the DB record
 * when this embedding was loaded from stored data (null when freshly extracted from
 * an incoming recognition request).
 */
@Getter
@Builder
public class FaceEmbedding {

    /** The feature vector. */
    private final float[] vector;

    /** Face profile this embedding belongs to (null for query embeddings). */
    private final Long faceProfileId;

    /** Face image record this embedding belongs to (null for query embeddings). */
    private final Long faceImageId;

    /** Person ID associated with this embedding (null for query embeddings). */
    private final Long personId;
}
